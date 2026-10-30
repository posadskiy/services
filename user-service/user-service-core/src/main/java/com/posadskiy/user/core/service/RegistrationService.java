package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.exception.AuthException;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import com.posadskiy.user.core.utils.PasswordEncoder;
import jakarta.inject.Singleton;

import java.util.Objects;
import java.util.Optional;

@Singleton
public class RegistrationService {
    private final UserEntityMapper userEntityMapper;
    private final UsersRepository userRepository;

    public RegistrationService(UserEntityMapper userEntityMapper, UsersRepository userRepository) {
        this.userEntityMapper = userEntityMapper;
        this.userRepository = userRepository;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public User registration(User user) {
        Objects.requireNonNull(user, "user");
        final Optional<UserEntity> byEmail = userRepository.findByEmail(user.email());
        if (byEmail.isPresent()) throw new AuthException("User already exists");

        var encodedPassword = isBlank(user.password()) ? null : PasswordEncoder.encode(user.password());
        var createdVia = isBlank(user.createdVia()) ? (encodedPassword == null ? "oauth" : "local") : user.createdVia();

        var encodedPasswordUser = new User(
            user.id(),
            user.email(),
            encodedPassword,
            false,
            user.pictureUrl(),
            createdVia,
            user.authProviders(),
            true,
            null,
            user.displayName(),
            user.locale(),
            user.timeZone(),
            user.avatarUrl());

        UserEntity savedUser = userRepository.save(userEntityMapper.mapToEntity(encodedPasswordUser));
        return userEntityMapper.mapFromEntity(savedUser);
    }
}
