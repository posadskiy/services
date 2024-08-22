package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.mapper.entity.UserEntityMapper;
import com.posadskiy.auth.core.model.User;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import com.posadskiy.auth.core.utils.PasswordEncoder;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Optional;

@Singleton
@NoArgsConstructor
public class RegistrationService {
    private UserEntityMapper userEntityMapper;
    private UsersRepository userRepository;

    public RegistrationService(UserEntityMapper userEntityMapper, UsersRepository userRepository) {
        this.userEntityMapper = userEntityMapper;
        this.userRepository = userRepository;
    }

    public User registration(@NonNull User user) {
        final Optional<UserEntity> byEmail = userRepository.findByEmailOrUsername(user.email(), user.username());
        if (byEmail.isPresent()) throw new AuthException("User already exists");

        var encodedPassword = PasswordEncoder.encode(user.password());
        var encodedPasswordUser = new User(user.id(), user.username(), user.email(), encodedPassword);

        UserEntity savedUser = userRepository.save(
            userEntityMapper.mapToEntity(encodedPasswordUser)
        );

        return userEntityMapper.mapFromEntity(
            savedUser
        );
    }
}
