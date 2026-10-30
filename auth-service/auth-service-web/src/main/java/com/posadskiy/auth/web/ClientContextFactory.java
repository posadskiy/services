package com.posadskiy.auth.web;

import com.posadskiy.auth.core.service.ClientContext;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;

public final class ClientContextFactory {

    private ClientContextFactory() {
    }

    public static ClientContext from(HttpRequest<?> request, @Nullable String linkOrigin) {
        return new ClientContext(ClientAddresses.key(request), request.getHeaders().get("User-Agent"), linkOrigin);
    }
}
