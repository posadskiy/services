package com.posadskiy.email.template;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
    info = @Info(
        title = "email-template-service",
        version = "${project.version}",
        description = "Email template service for managing and rendering email templates",
        license = @License(name = "MIT", url = "https://opensource.org/license/mit"),
        contact = @Contact(url = "https://posadskiy.com", name = "Dimitri Posadskiy", email = "support@posadskiy.com")
    )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
