package mk.ukim.finki.backend.service.parser.dicom;

import mk.ukim.finki.backend.domain.enums.EcgFormat;
import mk.ukim.finki.backend.domain.model.ParsedEcgRecord;
import mk.ukim.finki.backend.domain.model.PreparedEcgUpload;
import mk.ukim.finki.backend.service.interfaces.EcgFormatAdapter;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DicomFormatAdapter implements EcgFormatAdapter {

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

        ParsedEcgRecord record =
                waveformParser.parse(anonymizedDataset);

        String anonymizedSopInstanceUid =
                anonymizedDataset.getString(
                        Tag.SOPInstanceUID
                );

        String safeFilename =
                "dicom-" +
                        anonymizedSopInstanceUid +
                        ".dcm";

        return new PreparedEcgUpload(
                EcgFormat.DICOM,
                safeFilename,
                anonymizedContent,
                List.of(record)
        );
    }
}
