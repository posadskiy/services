package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.service.UserService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.tracing.annotation.ContinueSpan;
import lombok.NoArgsConstructor;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Secured(SecurityRule.IS_AUTHENTICATED)
@NoArgsConstructor
@ExecuteOn(TaskExecutors.VIRTUAL)
@Controller("v0/user")
public class UserController {
    private UserService userService;
    private UserDtoMapper userDtoMapper;

    public UserController(UserService userService, UserDtoMapper userDtoMapper) {
        this.userService = userService;
        this.userDtoMapper = userDtoMapper;
    }

    @Get("{id}")
    @ContinueSpan
    public UserDto getUserById(@Header(AUTHORIZATION) String authorization, @PathVariable Long id) {
        return userDtoMapper.mapToDto(
            userService.getUserById(id)
        );
    }
}
