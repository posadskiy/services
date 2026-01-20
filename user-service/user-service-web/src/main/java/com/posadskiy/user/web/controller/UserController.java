package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.UpdateUsernameRequest;
import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.service.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.tracing.annotation.ContinueSpan;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.VIRTUAL)
@Controller("v0/user")
public class UserController {
    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    public UserController(UserService userService, UserDtoMapper userDtoMapper) {
        this.userService = userService;
        this.userDtoMapper = userDtoMapper;
    }

    /**
     * Get current authenticated user information.
     * Extracts user ID from JWT token subject claim and returns user details.
     * This endpoint provides a convenient way for authenticated users to retrieve their own information
     * without needing to know their user ID.
     *
     * @param authentication The authenticated user's authentication object containing JWT claims
     * @return UserDto containing the current user's information
     * @throws IllegalArgumentException if the user ID in the token is invalid
     */
    @Get("/me")
    @ContinueSpan
    public UserDto getCurrentUser(Authentication authentication) {
        String userIdStr = authentication.getName();
        try {
            Long userId = Long.parseLong(userIdStr);
            return userDtoMapper.mapToDto(userService.getUserById(userId));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID in authentication token: " + userIdStr, e);
        }
    }

    @Get("{id}")
    @ContinueSpan
    public UserDto getUserById(@Header(AUTHORIZATION) String authorization, @PathVariable Long id) {
        return userDtoMapper.mapToDto(
            userService.getUserById(id)
        );
    }

    @Put("{id}")
    @ContinueSpan
    public UserDto updateUser(
        @Header(AUTHORIZATION) String authorization,
        @PathVariable Long id,
        @Body UpdateUsernameRequest request) {
        return userDtoMapper.mapToDto(
            userService.updateUser(id, request.username())
        );
    }

    /**
     * GDPR-compliant user deletion endpoint.
     * Permanently anonymizes user data according to GDPR Article 17
     * (Right to Erasure / "Right to be Forgotten").
     * <p>
     * Default behavior: Always anonymizes user data (removes PII while preserving structure for analytics).
     * This is GDPR-compliant and maintains data integrity.
     * <p>
     * For hard delete (complete removal), use POST /v0/user/{id}/hard-delete endpoint.
     *
     * @param id User ID to delete
     */
    @Delete("{id}")
    @ContinueSpan
    public HttpResponse<?> deleteUser(
        @Header(AUTHORIZATION) String authorization,
        @PathVariable Long id) {
        userService.deleteUser(id);
        return HttpResponse.noContent();
    }
}
