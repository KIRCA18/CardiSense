package mk.ukim.finki.backend.service.parser.dicom;

import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import org.dcm4che3.data.Attributes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DicomFormatAdapter implements EcgFormatAdapter {

    private static final Pattern STORED_DICOM_FILENAME = Pattern.compile(
            "^dicom-([0-9]+(?:\\.[0-9]+)*)\\.dcm$",
            Pattern.CASE_INSENSITIVE
    );

    private final DicomAnonymizationService anonymizationService;
    private final DicomFileService dicomFileService;
    private final DicomWaveformParser waveformParser;

    public DicomFormatAdapter(
            DicomAnonymizationService anonymizationService,
            DicomFileService dicomFileService,
            DicomWaveformParser waveformParser
    ) {
        this.anonymizationService = anonymizationService;
        this.dicomFileService = dicomFileService;
        this.waveformParser = waveformParser;
    }

    @Override
    public boolean supports(String filename) {
        return filename != null &&
                filename.toLowerCase(Locale.ROOT)
                        .endsWith(".dcm");
    }

    @Override
    public PreparedEcgUpload prepare(
            String filename,
            byte[] content
    ) {

        byte[] anonymizedContent =
                anonymizationService.anonymize(content);

        Attributes anonymizedDataset =
                dicomFileService.read(anonymizedContent);

        ParsedEcgRecord parsedRecord =
                waveformParser.parse(anonymizedDataset);

        String recordName = resolveRecordName(
                filename,
                parsedRecord.recordName()
        );

        ParsedEcgRecord record = new ParsedEcgRecord(
                recordName,
                parsedRecord.ecg()
        );

        String safeFilename =
                "dicom-" +
                        recordName +
                        ".dcm";

        return new PreparedEcgUpload(
                EcgFormat.DICOM,
                safeFilename,
                anonymizedContent,
                List.of(record)
        );
    }

    private String resolveRecordName(
            String filename,
            String anonymizedSopInstanceUid
    ) {
        String normalized = filename.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        Matcher matcher = STORED_DICOM_FILENAME.matcher(fileName);

        if (matcher.matches()) {
            return matcher.group(1);
        }

        return anonymizedSopInstanceUid;
    }
}
