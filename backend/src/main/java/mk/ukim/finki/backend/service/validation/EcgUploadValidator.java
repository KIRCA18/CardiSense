package mk.ukim.finki.backend.service.validation;

import mk.ukim.finki.backend.web.request.AnalyzeEcgRequest;
import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class EcgUploadValidator {

    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;
    private static final List<String> SUPPORTED_UPLOAD_SUFFIXES = List.of(
            ".dcm",
            ".scp",
            ".zip",
            ".rar",
            ".7z",
            ".tar",
            ".tar.gz",
            ".tgz"
    );

    public void validate(AnalyzeEcgRequest request) {
        if (request == null) {
            throw new InvalidEcgException("ECG analysis request is required");
        }

        validateUpload(request.originalFilename(), request.content(), request.declaredSize());
        validateModel(request.modelId());
    }

    public void validateUpload(String filename, byte[] content, long declaredSize) {
        validateContent(content, declaredSize);
        validateFilename(filename);
    }

    private void validateContent(byte[] content, long declaredSize) {
        if (content == null || content.length == 0) {
            throw new InvalidEcgException("Uploaded ECG file is empty");
        }

        if (content.length > MAX_UPLOAD_BYTES || declaredSize > MAX_UPLOAD_BYTES) {
            throw new InvalidEcgException("Uploaded ECG file exceeds 50 MB");
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidEcgException("Uploaded ECG must have a filename");
        }

        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        boolean supported = SUPPORTED_UPLOAD_SUFFIXES.stream()
                .anyMatch(normalizedFilename::endsWith);

        if (!supported) {
            throw new InvalidEcgException(
                    "Upload must be a DICOM .dcm file, an SCP-ECG .scp file, " +
                            "or a supported DICOM/SCP-ECG/WFDB archive"
            );
        }
    }

    private void validateModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new InvalidEcgException("A model ID is required");
        }
    }
}
