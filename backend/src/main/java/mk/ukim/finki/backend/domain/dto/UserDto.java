package mk.ukim.finki.backend.domain.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String firstName,
        String lastName
) {}
