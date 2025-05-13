package com.posadskiy.user.core.mapper.dto;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.model.User;
import io.micronaut.context.annotation.Mapper;
import jakarta.inject.Singleton;

@Singleton
public interface UserDtoMapper {
    @Mapper
    User mapFromDto(UserDto user);

    @Mapper
    UserDto mapToDto(User user);
}
