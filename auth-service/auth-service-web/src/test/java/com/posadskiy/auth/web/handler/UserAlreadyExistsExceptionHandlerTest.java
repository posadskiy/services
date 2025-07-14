package com.posadskiy.auth.web.handler;

import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.ErrorMessage;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class UserAlreadyExistsExceptionHandlerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void shouldHandleAuthException() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<?> response = handler.handle(HttpRequest.GET("/"), authException);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldReturnCorrectStatus() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<?> response = handler.handle(HttpRequest.GET("/"), authException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
    }

    @Test
    void shouldReturnErrorMessageInBody() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<ErrorMessage> response = handler.handle(HttpRequest.GET("/"), authException);

        // Then
        assertNotNull(response.getBody());
        ErrorMessage body = response.getBody().get();
        assertEquals(false, body.status());
        assertEquals(errorMessage, body.message());
    }

    @Test
    void shouldHandleEmptyMessage() {
        // Given
        String errorMessage = "";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<ErrorMessage> response = handler.handle(HttpRequest.GET("/"), authException);

        // Then
        assertNotNull(response.getBody());
        ErrorMessage body = response.getBody().get();
        assertEquals(false, body.status());
        assertEquals(errorMessage, body.message());
    }

    @Test
    void shouldHandleNullMessage() {
        // Given
        AuthException authException = new AuthException(null);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<ErrorMessage> response = handler.handle(HttpRequest.GET("/"), authException);

        // Then
        assertNotNull(response.getBody());
        ErrorMessage body = response.getBody().get();
        assertEquals(false, body.status());
        assertNull(body.message());
    }

    @Test
    void shouldHandleDifferentHttpRequests() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<?> getResponse = handler.handle(HttpRequest.GET("/"), authException);
        HttpResponse<?> postResponse = handler.handle(HttpRequest.POST("/", "data"), authException);
        HttpResponse<?> putResponse = handler.handle(HttpRequest.PUT("/", "data"), authException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, getResponse.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, postResponse.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, putResponse.getStatus());
    }

    @Test
    void shouldHandleRequestWithHeaders() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();
        HttpRequest<?> request = HttpRequest.GET("/")
                .header("Accept", MediaType.APPLICATION_JSON)
                .header("User-Agent", "test-client");

        // When
        HttpResponse<?> response = handler.handle(request, authException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldHandleRequestWithQueryParameters() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();
        HttpRequest<?> request = HttpRequest.GET("/?param=value");

        // When
        HttpResponse<?> response = handler.handle(request, authException);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertNotNull(response.getBody());
    }

    @Test
    void shouldCreateCorrectErrorMessage() {
        // Given
        String errorMessage = "User already exists";
        AuthException authException = new AuthException(errorMessage);
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        // When
        HttpResponse<ErrorMessage> response = handler.handle(HttpRequest.GET("/"), authException);

        // Then
        ErrorMessage body = response.getBody().get();
        assertNotNull(body);
        assertEquals(false, body.status());
        assertEquals(errorMessage, body.message());
    }
} 