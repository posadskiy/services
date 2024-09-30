package com.posadskiy.user.core.mapper.entity;

import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.model.User;
import io.micronaut.context.annotation.Mapper.Mapping;
import jakarta.inject.Singleton;

@Singleton
public interface UserEntityMapper {

    @Mapping(from = "id", to = "id")
    @Mapping(from = "username", to = "username")
    @Mapping(from = "email", to = "email")
    @Mapping(from = "passwordHash", to = "password")
    User mapFromEntity(UserEntity user);

    @Mapping(from = "id", to = "id")
    @Mapping(from = "username", to = "username")
    @Mapping(from = "email", to = "email")
    @Mapping(from = "password", to = "passwordHash")
    UserEntity mapToEntity(User user);
}
