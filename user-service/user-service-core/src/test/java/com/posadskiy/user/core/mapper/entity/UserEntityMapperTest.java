package com.posadskiy.user.core.mapper.entity;

import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class UserEntityMapperTest {

    @Test
    void testUserEntityMapperHasExpectedMethods() {
        assertNotNull(UserEntityMapper.class);
        try {
            UserEntityMapper.class.getMethod("mapFromEntity", UserEntity.class);
            UserEntityMapper.class.getMethod("mapToEntity", User.class);
        } catch (NoSuchMethodException e) {
            fail("UserEntityMapper should have mapFromEntity and mapToEntity methods");
        }
    }
}
