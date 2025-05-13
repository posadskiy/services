package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.model.User;
import com.posadskiy.user.core.service.RegistrationService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.tracing.annotation.NewSpan;
import lombok.NoArgsConstructor;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("v0/user/registration")
@NoArgsConstructor
public class RegistrationController {

    private RegistrationService registrationService;
    private UserDtoMapper userDtoMapper;

    public RegistrationController(RegistrationService registrationService, UserDtoMapper userDtoMapper) {
        this.registrationService = registrationService;
        this.userDtoMapper = userDtoMapper;
    }

    @Post("/signup")
    @NewSpan
    public UserDto registration(@Body final UserDto userDto) {
        final User user = userDtoMapper.mapFromDto(userDto);

        return userDtoMapper.mapToDto(
            registrationService.registration(user)
        );
    }
}
