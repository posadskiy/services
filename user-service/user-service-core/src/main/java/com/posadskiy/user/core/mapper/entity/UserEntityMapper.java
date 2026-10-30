package com.posadskiy.user.core.mapper.entity;

import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.model.User;
import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class UserEntityMapper {

    public User mapFromEntity(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new User(
            user.id(),
            user.email(),
            user.passwordHash(),
            user.emailVerified(),
            user.pictureUrl(),
            user.passwordHash() == null ? "oauth" : "local",
            List.of(),
            user.active(),
            user.deactivatedAt(),
            user.displayName(),
            user.locale(),
            user.timeZone(),
            user.avatarUrl());
    }

    public UserEntity mapToEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserEntity(
            user.id(),
            user.email(),
            UserEntity.normalizedEmail(user.email()),
            user.password(),
            Boolean.TRUE.equals(user.emailVerified()),
            user.pictureUrl(),
            user.displayName(),
            user.locale(),
            user.timeZone(),
            user.avatarUrl(),
            null,
            user.deactivatedAt(),
            user.active() != null ? user.active() : Boolean.TRUE,
            null,
            null,
            null);
    }
}
