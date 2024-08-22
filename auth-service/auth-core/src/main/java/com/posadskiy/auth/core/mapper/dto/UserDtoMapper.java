package com.posadskiy.auth.core.mapper.dto;

import com.posadskiy.auth.api.dto.UserDto;
import com.posadskiy.auth.core.model.User;
import io.micronaut.context.annotation.Mapper.Mapping;
import jakarta.inject.Singleton;

@Singleton
public interface UserDtoMapper {

    @Mapping(from = "id", to = "id")
    @Mapping(from = "username", to = "username")
    @Mapping(from = "email", to = "email")
    @Mapping(from = "password", to = "password")
    User mapFromDto(UserDto user);

    @Mapping(from = "id", to = "id")
    @Mapping(from = "username", to = "username")
    @Mapping(from = "email", to = "email") 
    @Mapping(from = "password", to = "password", condition = "#{false}")
    UserDto mapToDto(User user);
}
