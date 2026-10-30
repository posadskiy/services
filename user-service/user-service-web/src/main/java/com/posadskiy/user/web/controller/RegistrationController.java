package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.model.User;
import com.posadskiy.user.core.service.RegistrationService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.tracing.annotation.NewSpan;

/**
 * Account creation belongs to auth-service (captcha, password policy, email verification).
 * This endpoint is for trusted service callers only and never marks an email as verified.
 */
@Secured("ROLE_SERVICE")
@Controller
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserDtoMapper userDtoMapper;

    public RegistrationController(RegistrationService registrationService, UserDtoMapper userDtoMapper) {
        this.registrationService = registrationService;
        this.userDtoMapper = userDtoMapper;
    }

    @Post("/signup")
    @NewSpan
    public UserDto registration(@Body final UserDto userDto) {
        final User user = userDtoMapper.mapFromDto(userDto);

        return userDtoMapper.mapToDto(registrationService.registration(user));
    }
}
