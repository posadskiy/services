package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;

@Singleton
@NoArgsConstructor
public class UserService {
    private UserEntityMapper userEntityMapper;
    private UsersRepository userRepository;

    public UserService(UserEntityMapper userEntityMapper, UsersRepository userRepository) {
        this.userEntityMapper = userEntityMapper;
        this.userRepository = userRepository;
    }

    public User getUserById(String id) {
        return userEntityMapper.mapFromEntity(
            userRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("User with id " + id + " not found"))
        );
    }
}
