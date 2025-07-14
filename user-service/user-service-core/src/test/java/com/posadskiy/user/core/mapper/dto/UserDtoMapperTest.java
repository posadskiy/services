package com.posadskiy.user.core.mapper.dto;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserDtoMapperTest {

    @Test
    void testUserDtoMapperInterface() {
        // Test that the interface exists and has the expected methods
        assertNotNull(UserDtoMapper.class);
        assertTrue(UserDtoMapper.class.isInterface());
        
        // Verify the interface has the expected methods
        try {
            UserDtoMapper.class.getMethod("mapFromDto", UserDto.class);
            UserDtoMapper.class.getMethod("mapToDto", User.class);
        } catch (NoSuchMethodException e) {
            fail("UserDtoMapper interface should have mapFromDto and mapToDto methods");
        }
    }
} 