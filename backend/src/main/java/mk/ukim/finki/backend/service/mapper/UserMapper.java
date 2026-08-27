package mk.ukim.finki.backend.service.mapper;

import mk.ukim.finki.backend.domain.dto.UserResponse;
import mk.ukim.finki.backend.domain.entity.User;

public class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(final User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
