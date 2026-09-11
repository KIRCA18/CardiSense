package mk.ukim.finki.backend.service.parser.dicom;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import org.dcm4che3.data.UID;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomOutputStream;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dcm4che3.data.Attributes;

@Component
public class DicomFileService {


    public Attributes read(byte[] content) {
        try (
                DicomInputStream input = new DicomInputStream(
                        new ByteArrayInputStream(content)
                )
        ) {
            return input.readDataset();
        } catch (IOException exception) {
            throw new InvalidEcgException(
                    "Could not read the uploaded DICOM file",
                    exception
            );
        }
    }

    public byte[] write(Attributes dataset) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (
                DicomOutputStream output =
                        new DicomOutputStream(
                                bytes,
                                UID.ExplicitVRLittleEndian
                        )
        ) {
            Attributes fileMetaInformation =
                    dataset.createFileMetaInformation(
                            UID.ExplicitVRLittleEndian
                    );

            output.writeDataset(
                    fileMetaInformation,
                    dataset
            );
        } catch (IOException exception) {
            throw new InvalidEcgException(
                    "Could not create the anonymized DICOM file",
                    exception
            );
        }

        return bytes.toByteArray();
    }
}
