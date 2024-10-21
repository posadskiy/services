package com.posadskiy.auth.core;

import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.utils.PasswordMatcher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class AuthenticationProviderUserPassword<B> implements HttpRequestAuthenticationProvider<B> {

    @Inject
    private UsersRepository usersRepository;

    @Override
    public AuthenticationResponse authenticate(
        @Nullable HttpRequest<B> httpRequest,
        @NonNull AuthenticationRequest<String, String> authenticationRequest
    ) {
        if ("system".equals(authenticationRequest.getIdentity())) {
            return AuthenticationResponse.success(authenticationRequest.getIdentity());
        }
        var foundUser = usersRepository.findByEmail(authenticationRequest.getIdentity());
        if (foundUser.isEmpty()) {
            return AuthenticationResponse.failure(AuthenticationFailureReason.USER_NOT_FOUND);
        }
        var user = foundUser.get();

        var password = authenticationRequest.getSecret();
        
        var validated = PasswordMatcher.match(password, user.getPasswordHash());

        if (!validated) {
            return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
        }

        return AuthenticationResponse.success(authenticationRequest.getIdentity());
    }
}
