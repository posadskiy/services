package com.posadskiy.user.core.mapper.dto;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.model.User;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class UserDtoMapper {

    public User mapFromDto(UserDto dto) {
        if (dto == null) {
            return null;
        }
        List<String> providers = dto.authProviders() == null ? List.of() : dto.authProviders();
        return new User(
                dto.id(),
                dto.username(),
                dto.email(),
                dto.password(),
                dto.emailVerified(),
                dto.pictureUrl(),
                dto.createdVia(),
                providers,
                true, // active - default to true when creating from DTO
                null); // deactivatedAt - null for new users
    }

    public UserDto mapToDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.id(),
                user.username(),
                user.email(),
                null,
                user.emailVerified(),
                user.pictureUrl(),
                user.createdVia(),
                user.authProviders() == null ? List.of() : user.authProviders());
    }
}
