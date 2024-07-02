package com.posadskiy.auth.web.mapper;

import com.posadskiy.auth.api.SendEmailForm;
import com.posadskiy.auth.core.model.SendEmail;
import io.micronaut.context.annotation.Mapper;
import jakarta.inject.Singleton;

@Singleton
public interface EmailMapper {

    @Mapper
    SendEmail toModel(SendEmailForm sendEmailDto);
}
