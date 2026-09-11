package mk.ukim.finki.backend.web.request;

import java.util.UUID;

public record AnalyzeEcgRequest(
        String originalFilename,
        long declaredSize,
        byte[] content,
        String modelId,
        UUID requestedByUserId
) {
}