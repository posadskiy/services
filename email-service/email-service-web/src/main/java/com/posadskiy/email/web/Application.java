package com.posadskiy.email.web;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
    info = @Info(
        title = "email-service",
        version = "${project.version}",
        description = "Simple email delivering service for sending text or HTML based messages",
        license = @License(name = "MIT", url = "https://opensource.org/license/mit"),
        contact = @Contact(url = "https://posadskiy.com", name = "Dimitri Posadskiy", email = "support@posadskiy.com")
    )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
