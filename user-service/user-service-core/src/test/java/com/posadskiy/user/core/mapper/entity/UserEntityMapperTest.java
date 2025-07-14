package com.posadskiy.user.core.mapper.entity;

import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserEntityMapperTest {

    @Test
    void testUserEntityMapperInterface() {
        // Test that the interface exists and has the expected methods
        assertNotNull(UserEntityMapper.class);
        assertTrue(UserEntityMapper.class.isInterface());
        
        // Verify the interface has the expected methods
        try {
            UserEntityMapper.class.getMethod("mapFromEntity", UserEntity.class);
            UserEntityMapper.class.getMethod("mapToEntity", User.class);
        } catch (NoSuchMethodException e) {
            fail("UserEntityMapper interface should have mapFromEntity and mapToEntity methods");
        }
    }
} 