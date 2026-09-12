package mk.ukim.finki.backend.domain.dto;

public record ModelResponse(
        String key,
        String name,
        String version,
        String description,
        String device,
        String checkpoint
) {
}
