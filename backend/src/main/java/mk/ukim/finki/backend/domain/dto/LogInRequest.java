package mk.ukim.finki.backend.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record LogInRequest (
        @NotBlank String email,
        @NotBlank String password
) {}
