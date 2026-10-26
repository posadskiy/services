package com.posadskiy.user.core.mapper.dto;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class UserDtoMapperTest {

    @Test
    void testUserDtoMapperHasExpectedMethods() {
        assertNotNull(UserDtoMapper.class);
        try {
            UserDtoMapper.class.getMethod("mapFromDto", UserDto.class);
            UserDtoMapper.class.getMethod("mapToDto", User.class);
        } catch (NoSuchMethodException e) {
            fail("UserDtoMapper should have mapFromDto and mapToDto methods");
        }
    }
}
