package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.ExternalIdentityRepository;
import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class UserService {
    private UserEntityMapper userEntityMapper;
    private UsersRepository userRepository;
    private ExternalIdentityRepository externalIdentityRepository;

    public UserService(
            UserEntityMapper userEntityMapper,
            UsersRepository userRepository,
            ExternalIdentityRepository externalIdentityRepository) {
        this.userEntityMapper = userEntityMapper;
        this.userRepository = userRepository;
        this.externalIdentityRepository = externalIdentityRepository;
    }

    public User getUserById(Long id) {
        var entity = userRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("User with id " + id + " not found"));
        User baseUser = userEntityMapper.mapFromEntity(entity);
        List<String> providers = externalIdentityRepository.findByUserId(id).stream()
                .map(identity -> identity.getProvider().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
        return new User(
                baseUser.id(),
                baseUser.username(),
                baseUser.email(),
                baseUser.password(),
                baseUser.emailVerified(),
                baseUser.pictureUrl(),
                baseUser.createdVia(),
                providers);
    }
}
