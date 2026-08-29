package mk.ukim.finki.backend.web.request;

public record AnalyzeEcgRequest(
        String originalFilename,
        long declaredSize,
        byte[] content,
        String modelId
) {
}
