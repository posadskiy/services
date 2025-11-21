package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.LinkSocialIdentityRequest;
import com.posadskiy.user.api.SocialIdentityDto;
import com.posadskiy.user.core.service.SocialIdentityService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
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

    @Get
    public List<SocialIdentityDto> listIdentities(@PathVariable Long userId) {
        return socialIdentityService.listIdentities(userId);
    }

    @Post
    public SocialIdentityDto linkIdentity(
            @PathVariable Long userId, @Body @Valid LinkSocialIdentityRequest request) {
        return socialIdentityService.linkIdentity(userId, request);
    }

    @Delete("/{provider}")
    public HttpResponse<Void> unlinkIdentity(@PathVariable Long userId, @PathVariable String provider) {
        socialIdentityService.unlinkIdentity(userId, provider);
        return HttpResponse.noContent();
    }
}

