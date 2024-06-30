package com.posadskiy.email;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import lombok.Data;

@Data
@Singleton
public class Config {

    @Value("${email.username}")
    private String username;

    @Value("${email.password}")
    private String password;

    @Value("${email.protocol}")
    private String protocol;

    @Value("${email.host}")
    private String host;

    @Value("${email.port}")
    private String port;

    @Value("${email.auth}")
    private String auth;

    @Value("${email.starttls.enable}")
    private String startTlsEnable;

    @Value("${email.debug}")
    private String debug;
}
