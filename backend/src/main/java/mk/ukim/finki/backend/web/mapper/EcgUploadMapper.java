package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.exception.UploadReadException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;
import java.util.UUID;

@Component
public class EcgUploadMapper {

    public AnalyzeEcgRequest toRequest(
            List<MultipartFile> files,
            String modelId,
            UUID userId
    ) {
        if (files == null || files.size() != 1) {
            throw new InvalidEcgException(
                    "Exactly one ECG file or dataset archive must be uploaded. " +
                            "To analyze multiple ECG files, place them in one supported archive."
            );
        }

        MultipartFile file = files.get(0);

        if (file == null) {
            throw new InvalidEcgException(
                    "Exactly one ECG file or dataset archive must be uploaded"
            );
        }

        try {
            return new AnalyzeEcgRequest(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getBytes(),
                    modelId,
                    userId
            );
        } catch (IOException exception) {
            throw new UploadReadException(
                    "Could not read uploaded ECG file",
                    exception
            );
        }
    }
}
