package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.exception.AuthException;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import com.posadskiy.user.core.utils.PasswordEncoder;
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
