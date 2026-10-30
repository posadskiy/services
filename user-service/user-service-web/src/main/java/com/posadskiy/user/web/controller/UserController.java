package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.ProductPreferenceDto;
import com.posadskiy.user.api.UpdateProfileRequest;
import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.api.UserIds;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.service.ProductPreferenceService;
import com.posadskiy.user.core.service.UserService;
import com.posadskiy.user.web.AccountAccess;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.tracing.annotation.ContinueSpan;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.VIRTUAL)
@Controller("v0/user")
public class UserController {
    private final UserService userService;
    private final UserDtoMapper userDtoMapper;
    private final ProductPreferenceService productPreferenceService;

    public UserController(UserService userService, UserDtoMapper userDtoMapper, ProductPreferenceService productPreferenceService) {
        this.userService = userService;
        this.userDtoMapper = userDtoMapper;
        this.productPreferenceService = productPreferenceService;
    }

    private static Long parseUserId(String id) {
        try {
            return UserIds.parse(id);
        } catch (IllegalArgumentException e) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Get current authenticated user information.
     * Extracts user ID from JWT token subject claim and returns user details.
     * This endpoint provides a convenient way for authenticated users to retrieve their own information
     * without needing to know their user ID.
     *
     * @param authentication The authenticated user's authentication object containing JWT claims
     * @return UserDto containing the current user's information
     * @throws io.micronaut.http.exceptions.HttpStatusException if the user ID in the token is invalid
     */
    @Get("/me")
    @ContinueSpan
    public UserDto getCurrentUser(Authentication authentication) {
        return userDtoMapper.mapToDto(userService.getUserById(parseUserId(authentication.getName())));
    }

    @Get("{id}")
    @ContinueSpan
    public UserDto getUserById(Authentication authentication, @PathVariable String id) {
        AccountAccess.selfOrService(authentication, id);
        return userDtoMapper.mapToDto(userService.getUserById(parseUserId(id)));
    }

    @Patch("/me")
    @ContinueSpan
    public UserDto updateProfile(Authentication authentication, @Body UpdateProfileRequest request) {
        return userDtoMapper.mapToDto(userService.updateProfile(
            parseUserId(authentication.getName()),
            request.displayName(),
            request.locale(),
            request.timeZone(),
            request.avatarUrl()));
    }

    @Get("/me/preferences/{product}")
    @ContinueSpan
    public ProductPreferenceDto preference(Authentication authentication, @PathVariable String product) {
        return productPreferenceService.get(parseUserId(authentication.getName()), product);
    }

    @Put("/me/preferences/{product}")
    @ContinueSpan
    public ProductPreferenceDto savePreference(
        Authentication authentication, @PathVariable String product, @Body ProductPreferenceDto request) {
        return productPreferenceService.save(
            parseUserId(authentication.getName()), product, request.marketingEmail(), request.settings());
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
    public HttpResponse<?> deleteUser(Authentication authentication, @PathVariable String id) {
        AccountAccess.selfOrService(authentication, id);
        userService.deleteUser(parseUserId(id));
        return HttpResponse.noContent();
    }
}
