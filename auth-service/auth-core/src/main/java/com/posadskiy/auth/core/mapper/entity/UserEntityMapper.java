package com.posadskiy.auth.core.mapper.entity;

import com.posadskiy.auth.core.model.User;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
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
