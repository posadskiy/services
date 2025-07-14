package com.posadskiy.auth.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMessageTest {

    @Test
    void shouldCreateErrorMessageWithValidData() {
        // Given
        Boolean status = false;
        String message = "User authentication failed";

        // When
        ErrorMessage errorMessage = new ErrorMessage(status, message);

        // Then
        assertNotNull(errorMessage);
        assertEquals(status, errorMessage.status());
        assertEquals(message, errorMessage.message());
    }

    @Test
    void shouldCreateErrorMessageWithTrueStatus() {
        // Given
        Boolean status = true;
        String message = "Success message";

        // When
        ErrorMessage errorMessage = new ErrorMessage(status, message);

        // Then
        assertEquals(status, errorMessage.status());
        assertEquals(message, errorMessage.message());
    }

    @Test
    void shouldCreateErrorMessageWithNullStatus() {
        // Given
        Boolean status = null;
        String message = "Test message";

        // When
        ErrorMessage errorMessage = new ErrorMessage(status, message);

        // Then
        assertNull(errorMessage.status());
        assertEquals(message, errorMessage.message());
    }

    @Test
    void shouldCreateErrorMessageWithNullMessage() {
        // Given
        Boolean status = false;
        String message = null;

        // When
        ErrorMessage errorMessage = new ErrorMessage(status, message);

        // Then
        assertEquals(status, errorMessage.status());
        assertNull(errorMessage.message());
    }

    @Test
    void shouldCreateErrorMessageWithEmptyMessage() {
        // Given
        Boolean status = false;
        String message = "";

        // When
        ErrorMessage errorMessage = new ErrorMessage(status, message);

        // Then
        assertEquals(status, errorMessage.status());
        assertEquals(message, errorMessage.message());
    }

    @Test
    void shouldHaveCorrectRecordBehavior() {
        // Given
        Boolean status = false;
        String message = "Test message";

        // When
        ErrorMessage errorMessage1 = new ErrorMessage(status, message);
        ErrorMessage errorMessage2 = new ErrorMessage(status, message);

        // Then
        assertEquals(errorMessage1, errorMessage2);
        assertEquals(errorMessage1.hashCode(), errorMessage2.hashCode());
    }

    @Test
    void shouldHaveDifferentHashCodesForDifferentValues() {
        // Given
        ErrorMessage errorMessage1 = new ErrorMessage(false, "Message 1");
        ErrorMessage errorMessage2 = new ErrorMessage(true, "Message 2");

        // When & Then
        assertNotEquals(errorMessage1.hashCode(), errorMessage2.hashCode());
    }

    @Test
    void shouldHaveMeaningfulToString() {
        // Given
        Boolean status = false;
        String message = "Test message";
        ErrorMessage errorMessage = new ErrorMessage(status, message);

        // When
        String toString = errorMessage.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("ErrorMessage"));
        assertTrue(toString.contains("status=" + status));
        assertTrue(toString.contains("message=" + message));
    }
} 