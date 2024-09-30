package com.posadskiy.user.core.web.controller;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.service.UserService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import lombok.NoArgsConstructor;

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("v0/user")
@NoArgsConstructor
public class UserController {
    private UserService userService;
    private UserDtoMapper userDtoMapper;

    public UserController(UserService userService, UserDtoMapper userDtoMapper) {
        this.userService = userService;
        this.userDtoMapper = userDtoMapper;
    }

    @Get("{id}")
    public UserDto getUserById(String id) {
        return userDtoMapper.mapToDto(
            userService.getUserById(id)
        );
    }
}
