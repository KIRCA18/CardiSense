package mk.ukim.finki.backend.service.validation;

import mk.ukim.finki.backend.domain.dto.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class EcgUploadValidator {

    private static final long MAX_ARCHIVE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_MODELS = Set.of("ecg-fm");

    public void validate(AnalyzeEcgRequest request) {
        if (request == null) {
            throw new InvalidEcgException("ECG analysis request is required");
        }

        validateContent(request);
        validateFilename(request.originalFilename());
        validateModel(request.modelId());
    }

    private void validateContent(AnalyzeEcgRequest request) {
        byte[] content = request.content();

        if (content == null || content.length == 0) {
            throw new InvalidEcgException("Uploaded ECG archive is empty");
        }

        if (content.length > MAX_ARCHIVE_BYTES || request.declaredSize() > MAX_ARCHIVE_BYTES) {
            throw new InvalidEcgException("Uploaded ECG archive exceeds 10 MB");
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidEcgException("Uploaded ECG archive must have a filename");
        }

        if (!filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new InvalidEcgException("Uploaded ECG must be a ZIP archive");
        }
    }

    private void validateModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new InvalidEcgException("A model ID is required");
        }

        if (!SUPPORTED_MODELS.contains(modelId)) {
            throw new InvalidEcgException("Unsupported model: " + modelId);
        }
    }
}
