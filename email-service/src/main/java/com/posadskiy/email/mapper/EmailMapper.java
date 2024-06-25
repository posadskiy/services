package com.posadskiy.email.mapper;

import com.posadskiy.email.dto.SendEmailForm;
import com.posadskiy.email.model.SendEmail;
import io.micronaut.context.annotation.Mapper;
import jakarta.inject.Singleton;

@Singleton
public interface EmailMapper {

    @Mapper
    SendEmail toModel(SendEmailForm sendEmailDto);
}
