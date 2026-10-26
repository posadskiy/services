package com.posadskiy.email.web.mapper;

import com.posadskiy.email.api.SendEmailForm;
import com.posadskiy.email.model.SendEmail;
import io.micronaut.context.annotation.Mapper;
import jakarta.inject.Singleton;

@Singleton
public interface EmailMapper {

    @Mapper
    SendEmail toModel(SendEmailForm sendEmailDto);
}
