package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.UserDto;
import com.posadskiy.auth.core.mapper.dto.UserDtoMapper;
import com.posadskiy.auth.core.model.User;
import com.posadskiy.auth.core.service.RegistrationService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import lombok.NoArgsConstructor;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("v0/registration")
@NoArgsConstructor
public class RegistrationController {

    private RegistrationService registrationService;
    private UserDtoMapper userDtoMapper;

    public RegistrationController(RegistrationService registrationService, UserDtoMapper userDtoMapper) {
        this.registrationService = registrationService;
        this.userDtoMapper = userDtoMapper;
    }

    @Post("signup")
    public UserDto registration(@Body final UserDto userDto) {
        final User user = userDtoMapper.mapFromDto(userDto);

        return userDtoMapper.mapToDto(
            registrationService.registration(user)
        );
    }
}
