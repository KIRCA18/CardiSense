package mk.ukim.finki.backend.service.implementation;

import jakarta.transaction.Transactional;
import mk.ukim.finki.backend.domain.entity.*;
import mk.ukim.finki.backend.domain.exception.ModelInferenceException;
import mk.ukim.finki.backend.domain.model.*;
import mk.ukim.finki.backend.repository.*;
import mk.ukim.finki.backend.service.interfaces.*;
import mk.ukim.finki.backend.service.validation.EcgUploadValidator;
import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class EcgAnalysisServiceImpl implements EcgAnalysisService {

    private final EcgUploadValidator uploadValidator;
    private final EcgArchiveParser archiveParser;
    private final EcgAnonymizationService anonymizationService;
    private final ModelInferenceService modelInferenceService;

    private final EcgUploadRepository uploadRepository;
    private final EcgRecordRepository ecgRecordRepository;
    private final EcgRecordOwnershipRepository ownershipRepository;
    private final AnalysisRepository analysisRepository;
    private final MlModelRepository mlModelRepository;
    private final UserRepository userRepository;

    public EcgAnalysisServiceImpl(
            EcgUploadValidator uploadValidator,
            EcgArchiveParser archiveParser,
            EcgAnonymizationService anonymizationService,
            ModelInferenceService modelInferenceService,
            EcgUploadRepository uploadRepository,
            EcgRecordRepository ecgRecordRepository,
            EcgRecordOwnershipRepository ownershipRepository,
            AnalysisRepository analysisRepository,
            MlModelRepository mlModelRepository,
            UserRepository userRepository
    ) {
        this.uploadValidator = uploadValidator;
        this.archiveParser = archiveParser;
        this.anonymizationService = anonymizationService;
        this.modelInferenceService = modelInferenceService;
        this.uploadRepository = uploadRepository;
        this.ecgRecordRepository = ecgRecordRepository;
        this.ownershipRepository = ownershipRepository;
        this.analysisRepository = analysisRepository;
        this.mlModelRepository = mlModelRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DatasetAnalysisResult analyze(AnalyzeEcgRequest request) {
        uploadValidator.validate(request);

        User user = userRepository.findById(request.requestedByUserId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user no longer exists"
                        )
                );

        MlModel model = mlModelRepository
                .findByModelKeyAndActiveTrue(request.modelId())
                .orElseThrow(() ->
                        new ModelInferenceException(
                                "Unknown or inactive model: " +
                                        request.modelId()
                        )
                );

        List<ParsedEcgRecord> parsedRecords = archiveParser.parse(
                request.originalFilename(),
                request.content()
        );

        EcgUpload upload = uploadRepository.save(
                new EcgUpload(
                        request.originalFilename(),
                        determineArchiveFormat(
                                request.originalFilename()
                        ),
                        request.declaredSize(),
                        request.content()
                )
        );

        List<DatasetAnalysisResult.RecordAnalysis> results =
                new ArrayList<>();

        for (ParsedEcgRecord parsedRecord : parsedRecords) {
            EcgRecord ecgRecord = saveEcgRecord(
                    upload,
                    parsedRecord
            );

            ownershipRepository.save(
                    new EcgRecordOwnership(ecgRecord, user)
            );

            CommonEcg commonEcg = anonymizationService.anonymize(
                    ecgRecord.getId(),
                    parsedRecord.ecg()
            );

            Instant startedAt = Instant.now();

            AnalysisResult analysisResult =
                    modelInferenceService.infer(
                            request.modelId(),
                            commonEcg
                    );

            Instant completedAt = Instant.now();

            Analysis analysis = Analysis.completed(
                    ecgRecord,
                    model,
                    user,
                    analysisResult,
                    startedAt,
                    completedAt
            );

            analysisRepository.save(analysis);

            results.add(
                    new DatasetAnalysisResult.RecordAnalysis(
                            parsedRecord.recordName(),
                            analysisResult
                    )
            );
        }

        return new DatasetAnalysisResult(results);
    }

    private EcgRecord saveEcgRecord(
            EcgUpload upload,
            ParsedEcgRecord parsedRecord
    ) {
        ParsedEcg parsed = parsedRecord.ecg();

        int sampleCount = parsed.waveform()[0].length;

        double durationSeconds =
                sampleCount / parsed.samplingRateHz();

        EcgRecord ecgRecord = new EcgRecord(
                upload,
                parsedRecord.recordName(),
                "WFDB",
                parsed.leadNames(),
                parsed.units(),
                sampleCount,
                parsed.samplingRateHz(),
                durationSeconds
        );

        return ecgRecordRepository.save(ecgRecord);
    }

    private String determineArchiveFormat(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);

        if (
                normalized.endsWith(".tar.gz") ||
                        normalized.endsWith(".tgz")
        ) {
            return "TAR_GZIP";
        }

        if (normalized.endsWith(".zip")) {
            return "ZIP";
        }

        if (normalized.endsWith(".rar")) {
            return "RAR";
        }

        if (normalized.endsWith(".7z")) {
            return "SEVEN_Z";
        }

        if (normalized.endsWith(".tar")) {
            return "TAR";
        }

        throw new IllegalArgumentException(
                "Unsupported archive format: " + filename
        );
    }
}