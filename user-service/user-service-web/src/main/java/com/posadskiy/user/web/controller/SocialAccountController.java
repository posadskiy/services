package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.LinkSocialIdentityRequest;
import com.posadskiy.user.api.SocialIdentityDto;
import com.posadskiy.user.api.UserIds;
import com.posadskiy.user.core.service.SocialIdentityService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;

import java.util.List;

@Controller("/v0/user/{userId}/identities")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class SocialAccountController {

    private final SocialIdentityService socialIdentityService;

    public SocialAccountController(SocialIdentityService socialIdentityService) {
        this.socialIdentityService = socialIdentityService;
    }

    private static Long parseUserId(String id) {
        try {
            return UserIds.parse(id);
        } catch (IllegalArgumentException e) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Get
    public List<SocialIdentityDto> listIdentities(@PathVariable String userId) {
        return socialIdentityService.listIdentities(parseUserId(userId));
    }

    @Post
    public SocialIdentityDto linkIdentity(@PathVariable String userId, @Body @Valid LinkSocialIdentityRequest request) {
        return socialIdentityService.linkIdentity(parseUserId(userId), request);
    }

    @Delete("/{provider}")
    public HttpResponse<Void> unlinkIdentity(@PathVariable String userId, @PathVariable String provider) {
        socialIdentityService.unlinkIdentity(parseUserId(userId), provider);
        return HttpResponse.noContent();
    }
}
