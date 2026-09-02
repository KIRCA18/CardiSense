package mk.ukim.finki.backend.service.parser.dicom;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.deident.DeIdentifier;
import org.springframework.stereotype.Service;

@Service
public class DicomAnonymizationService {

    private final DicomFileService dicomFileService;

    public DicomAnonymizationService(
            DicomFileService dicomFileService
    ) {
        this.dicomFileService = dicomFileService;
    }

    public byte[] anonymize(byte[] originalContent) {
        Attributes original = dicomFileService.read(
                originalContent
        );


        Attributes anonymized = new Attributes(
                original,
                false
        );

        DeIdentifier deIdentifier = new DeIdentifier();
        deIdentifier.deidentify(anonymized);


        anonymized.setString(
                Tag.PatientName,
                VR.PN,
                "ANONYMIZED"
        );

        anonymized.setString(
                Tag.PatientID,
                VR.LO,
                "ANONYMIZED"
        );

        return dicomFileService.write(anonymized);
    }
}