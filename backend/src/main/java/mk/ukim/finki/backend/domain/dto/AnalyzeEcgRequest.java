package mk.ukim.finki.backend.domain.dto;

public record AnalyzeEcgRequest(
        String originalFilename,
        long declaredSize,
        byte[] content,
        String modelId
) {
}
