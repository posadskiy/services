package com.posadskiy.user.core.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ErrorMessageTest {

    @Test
    void testErrorMessageRecord() {
        ErrorMessage errorMessage = new ErrorMessage(false, "User already exists");
        
        assertFalse(errorMessage.status());
        assertEquals("User already exists", errorMessage.message());
    }
    
    @Test
    void testErrorMessageRecordWithNullValues() {
        ErrorMessage errorMessage = new ErrorMessage(null, null);
        
        assertNull(errorMessage.status());
        assertNull(errorMessage.message());
    }
    
    @Test
    void testErrorMessageRecordEquality() {
        ErrorMessage error1 = new ErrorMessage(false, "User already exists");
        ErrorMessage error2 = new ErrorMessage(false, "User already exists");
        ErrorMessage error3 = new ErrorMessage(true, "User already exists");
        
        assertEquals(error1, error2);
        assertNotEquals(error1, error3);
        assertEquals(error1.hashCode(), error2.hashCode());
        assertNotEquals(error1.hashCode(), error3.hashCode());
    }
    
    @Test
    void testErrorMessageRecordToString() {
        ErrorMessage errorMessage = new ErrorMessage(false, "User already exists");
        String toString = errorMessage.toString();
        
        assertTrue(toString.contains("ErrorMessage"));
        assertTrue(toString.contains("status=false"));
        assertTrue(toString.contains("message=User already exists"));
    }
} 