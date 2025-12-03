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
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getEmailVerified(),
                user.getPictureUrl(),
                user.getPasswordHash() == null ? "oauth" : "local",
                List.of(),
                user.getActive() != null ? user.getActive() : Boolean.TRUE,
                user.getDeactivatedAt());
    }

    public UserEntity mapToEntity(User user) {
        if (user == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setUsername(user.username());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.password());
        entity.setEmailVerified(user.emailVerified() != null ? user.emailVerified() : Boolean.FALSE);
        entity.setPictureUrl(user.pictureUrl());
        entity.setActive(user.active() != null ? user.active() : Boolean.TRUE);
        entity.setDeactivatedAt(user.deactivatedAt());
        return entity;
    }
}
