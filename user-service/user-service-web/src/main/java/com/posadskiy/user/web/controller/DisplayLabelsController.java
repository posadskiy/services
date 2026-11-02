package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.DisplayLabelsRequest;
import com.posadskiy.user.api.UserIds;
import com.posadskiy.user.core.service.UserService;
import com.posadskiy.user.web.AccountAccess;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.tracing.annotation.ContinueSpan;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Batch member labels for internal callers. Does not expose full profiles.
 */
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.VIRTUAL)
@Controller("v0/users")
public class DisplayLabelsController {
    private final UserService userService;

    public DisplayLabelsController(UserService userService) {
        this.userService = userService;
    }

    @Post("/display-labels")
    @ContinueSpan
    public Map<String, String> displayLabels(Authentication authentication, @Body @Valid DisplayLabelsRequest request) {
        AccountAccess.labelsReader(authentication);
        Set<Long> ids = new LinkedHashSet<>();
        for (String raw : request.ids()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                ids.add(UserIds.parse(raw.trim()));
            } catch (IllegalArgumentException e) {
                throw new HttpStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
        }
        List<Long> ordered = new ArrayList<>(ids);
        return userService.displayLabels(ordered);
    }
}
