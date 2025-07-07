package com.posadskiy.email.client;

import com.posadskiy.user.api.UserDto;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Client("${user.service.url:http://user-service.internal}")
public interface UserClient {
    
    @Get("v0/user/{id}")
    UserDto getUserById(@Header(AUTHORIZATION) String authorization, @PathVariable("id") String id);
}
