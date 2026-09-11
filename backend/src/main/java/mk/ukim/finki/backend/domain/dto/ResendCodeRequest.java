package mk.ukim.finki.backend.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendCodeRequest(
        @NotBlank @Email String email
) {}
