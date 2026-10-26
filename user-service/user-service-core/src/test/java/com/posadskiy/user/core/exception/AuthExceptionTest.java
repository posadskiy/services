package com.posadskiy.user.core.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthExceptionTest {

    @Test
    void testAuthExceptionWithMessage() {
        String message = "User already exists";
        AuthException exception = new AuthException(message);
        
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    void testAuthExceptionWithEmptyMessage() {
        String message = "";
        AuthException exception = new AuthException(message);
        
        assertEquals(message, exception.getMessage());
    }
    
    @Test
    void testAuthExceptionWithNullMessage() {
        AuthException exception = new AuthException(null);
        
        assertNull(exception.getMessage());
    }
    
    @Test
    void testAuthExceptionIsRuntimeException() {
        AuthException exception = new AuthException("test");
        
        assertTrue(exception instanceof RuntimeException);
    }
} 