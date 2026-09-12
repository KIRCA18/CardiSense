package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.dto.AnalysisEntryResponse;
import mk.ukim.finki.backend.domain.entity.*;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.exception.ModelInferenceException;
import mk.ukim.finki.backend.domain.exception.NotFoundException;
import mk.ukim.finki.backend.domain.model.*;
import mk.ukim.finki.backend.repository.*;
import mk.ukim.finki.backend.service.interfaces.*;
import mk.ukim.finki.backend.web.mapper.AnalysisEntryMapper;
import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EcgAnalysisServiceImpl implements EcgAnalysisService {

    private final DatasetIngestionService datasetIngestionService;
    private final EcgFormatAdapterSelector adapterSelector;
    private final EcgAnonymizationService anonymizationService;
    private final ModelInferenceService modelInferenceService;

    private final EcgRecordOwnershipRepository ownershipRepository;
    private final AnalysisRepository analysisRepository;
    private final MlModelRepository mlModelRepository;
    private final UserRepository userRepository;

    // --- Quick path: upload + immediately analyze against one model ---

    @Override
    @Transactional
    public DatasetAnalysisResult analyze(AnalyzeEcgRequest request) {
        if (request == null) {
            throw new InvalidEcgException("ECG analysis request is required");
        }
        if (request.originalFilename() == null || request.modelId() == null || request.modelId().isBlank()) {
            throw new InvalidEcgException("A model ID is required");
        }

        User user = requireUser(request.requestedByUserId());

        MlModel model = mlModelRepository
                .findByModelKeyAndActiveTrue(request.modelId())
                .orElseThrow(() -> new ModelInferenceException("Unknown or inactive model: " + request.modelId()));

        IngestedUpload ingested = datasetIngestionService.ingest(user, request.originalFilename(), request.content());

        // Re-parse once to recover the decoded waveform per record (ingestion only persists metadata).
        Map<String, ParsedEcgRecord> parsedByRecordName = parseUpload(
                ingested.upload().getOriginalFilename(),
                ingested.upload().getArchiveContent()
        );

        List<DatasetAnalysisResult.RecordAnalysis> results = new ArrayList<>();
        for (EcgRecord ecgRecord : ingested.records()) {
            ParsedEcgRecord parsedRecord = parsedByRecordName.get(ecgRecord.getRecordName().toLowerCase(Locale.ROOT));
            if (parsedRecord == null) {
                continue;
            }

            Instant startedAt = Instant.now();
            CommonEcg commonEcg = anonymizationService.anonymize(ecgRecord.getId(), parsedRecord.ecg());
            AnalysisResult analysisResult = modelInferenceService.infer(request.modelId(), commonEcg);
            Instant completedAt = Instant.now();

            analysisRepository.save(Analysis.completed(ecgRecord, model, user, analysisResult, startedAt, completedAt));

            results.add(new DatasetAnalysisResult.RecordAnalysis(parsedRecord.recordName(), analysisResult));
        }

        return new DatasetAnalysisResult(results);
    }

    // --- Two-phase path: analyze already-ingested records against one or more models ---

    @Override
    @Transactional
    public List<AnalysisEntryResponse> run(User user, List<UUID> recordIds, List<String> modelKeys) {
        List<UUID> distinctRecordIds = recordIds.stream().distinct().toList();
        List<EcgRecordOwnership> ownerships = ownershipRepository
                .findByEcgRecord_IdInAndUser_Id(distinctRecordIds, user.getId());

        if (ownerships.isEmpty()) {
            throw new NotFoundException("None of the selected records belong to your account");
        }

        Map<String, MlModel> models = new LinkedHashMap<>();
        for (String key : modelKeys.stream().distinct().toList()) {
            MlModel model = mlModelRepository.findByModelKeyAndActiveTrue(key)
                    .orElseThrow(() -> new InvalidEcgException("Unknown or inactive model: " + key));
            models.put(key, model);
        }

        // Re-parse each distinct upload archive once, even if multiple selected
        // records (or models) belong to the same dataset.
        Map<UUID, Map<String, ParsedEcgRecord>> parsedByUpload = new HashMap<>();

        List<Analysis> results = new ArrayList<>();
        for (EcgRecordOwnership ownership : ownerships) {
            EcgRecord ecgRecord = ownership.getEcgRecord();
            EcgUpload upload = ecgRecord.getUpload();

            Map<String, ParsedEcgRecord> parsedRecords = parsedByUpload.computeIfAbsent(
                    upload.getId(),
                    id -> parseUpload(upload.getOriginalFilename(), upload.getArchiveContent())
            );
            ParsedEcgRecord parsedRecord = parsedRecords.get(ecgRecord.getRecordName().toLowerCase(Locale.ROOT));

            for (MlModel model : models.values()) {
                results.add(runOne(user, ecgRecord, model, parsedRecord));
            }
        }

        return results.stream().map(AnalysisEntryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalysisEntryResponse> history(UUID userId) {
        return analysisRepository.findByRequestedBy_IdOrderByStartedAtDesc(userId).stream()
                .map(AnalysisEntryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisEntryResponse getForUser(UUID analysisId, UUID userId) {
        return analysisRepository.findByIdAndRequestedBy_Id(analysisId, userId)
                .map(AnalysisEntryMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Analysis not found"));
    }

    // --- helpers ---

    private Analysis runOne(User user, EcgRecord ecgRecord, MlModel model, ParsedEcgRecord parsedRecord) {
        Instant startedAt = Instant.now();

        if (parsedRecord == null) {
            return analysisRepository.save(Analysis.failed(
                    ecgRecord, model, user,
                    "Could not recover this record's waveform from its dataset archive",
                    startedAt, Instant.now()
            ));
        }

        try {
            CommonEcg commonEcg = anonymizationService.anonymize(ecgRecord.getId(), parsedRecord.ecg());
            AnalysisResult result = modelInferenceService.infer(model.getModelKey(), commonEcg);
            Instant completedAt = Instant.now();
            return analysisRepository.save(Analysis.completed(ecgRecord, model, user, result, startedAt, completedAt));
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            return analysisRepository.save(Analysis.failed(ecgRecord, model, user, message, startedAt, Instant.now()));
        }
    }

    private Map<String, ParsedEcgRecord> parseUpload(String originalFilename, byte[] archiveContent) {
        EcgFormatAdapter adapter = adapterSelector.select(originalFilename);
        PreparedEcgUpload prepared = adapter.prepare(originalFilename, archiveContent);

        Map<String, ParsedEcgRecord> byName = new HashMap<>();
        for (ParsedEcgRecord record : prepared.records()) {
            byName.put(record.recordName().toLowerCase(Locale.ROOT), record);
        }
        return byName;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
    }
}
