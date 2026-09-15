package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.entity.EcgRecord;
import mk.ukim.finki.backend.domain.entity.EcgRecordOwnership;
import mk.ukim.finki.backend.domain.entity.EcgUpload;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.model.IngestedUpload;
import mk.ukim.finki.backend.domain.model.ParsedEcg;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.repository.EcgRecordOwnershipRepository;
import mk.ukim.finki.backend.repository.EcgRecordRepository;
import mk.ukim.finki.backend.repository.EcgUploadRepository;
import mk.ukim.finki.backend.service.interfaces.DatasetIngestionService;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import mk.ukim.finki.backend.service.validation.EcgUploadValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DatasetIngestionServiceImpl implements DatasetIngestionService {

    private final EcgUploadValidator uploadValidator;
    private final EcgFormatAdapterSelector adapterSelector;
    private final EcgUploadRepository uploadRepository;
    private final EcgRecordRepository ecgRecordRepository;
    private final EcgRecordOwnershipRepository ownershipRepository;

    @Override
    @Transactional
    public IngestedUpload ingest(User owner, String originalFilename, byte[] content) {
        uploadValidator.validateUpload(originalFilename, content, content == null ? 0 : content.length);

        EcgFormatAdapter adapter = adapterSelector.select(originalFilename);
        PreparedEcgUpload prepared = adapter.prepare(originalFilename, content);

        EcgUpload upload = uploadRepository.save(new EcgUpload(
                prepared.filenameForStorage(),
                determineContainerFormat(prepared.filenameForStorage()),
                prepared.contentForStorage().length,
                prepared.contentForStorage()
        ));

        List<EcgRecord> records = new ArrayList<>();
        for (ParsedEcgRecord parsedRecord : prepared.records()) {
            EcgRecord record = saveEcgRecord(upload, parsedRecord, prepared.format());
            ownershipRepository.save(new EcgRecordOwnership(record, owner));
            records.add(record);
        }

        return new IngestedUpload(upload, records);
    }

    private EcgRecord saveEcgRecord(EcgUpload upload, ParsedEcgRecord parsedRecord, EcgFormat format) {
        ParsedEcg parsed = parsedRecord.ecg();
        int sampleCount = parsed.waveform()[0].length;
        double durationSeconds = sampleCount / parsed.samplingRateHz();

        EcgRecord ecgRecord = new EcgRecord(
                upload,
                parsedRecord.recordName(),
                format.name(),
                parsed.leadNames(),
                parsed.units(),
                sampleCount,
                parsed.samplingRateHz(),
                durationSeconds
        );

        return ecgRecordRepository.save(ecgRecord);
    }

    private String determineContainerFormat(String filename) {
        String normalized = filename.toLowerCase(Locale.ROOT);

        if (normalized.endsWith(".dcm") || normalized.endsWith(".scp")) {
            return "DIRECT";
        }
        if (normalized.endsWith(".tar.gz") || normalized.endsWith(".tgz")) {
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

        throw new IllegalArgumentException("Unsupported upload format: " + filename);
    }
}
