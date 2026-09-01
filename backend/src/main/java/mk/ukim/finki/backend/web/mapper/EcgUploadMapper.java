package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.exception.UploadReadException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.UUID;

@Component
public class EcgUploadMapper {

    public AnalyzeEcgRequest toRequest(
            MultipartFile file,
            String modelId,
            UUID requestedByUserId
    ) {
        try {
            return new AnalyzeEcgRequest(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getBytes(),
                    modelId,
                    requestedByUserId
            );
        } catch (IOException exception) {
            throw new UploadReadException(
                    "Could not read uploaded ECG archive",
                    exception
            );
        }
    }
}