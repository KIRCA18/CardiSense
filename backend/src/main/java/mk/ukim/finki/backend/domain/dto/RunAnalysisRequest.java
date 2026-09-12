package mk.ukim.finki.backend.domain.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record RunAnalysisRequest(
        @NotEmpty(message = "Select at least one record")
        List<UUID> recordIds,

        @NotEmpty(message = "Select at least one model")
        List<String> modelKeys
) {
}
