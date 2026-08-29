package mk.ukim.finki.backend.web.mapper;

import mk.ukim.finki.backend.domain.dto.UserDto;
import mk.ukim.finki.backend.domain.entity.User;

public class UserMapper {

    private UserMapper() {}

    public static UserDto toResponse(final User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
