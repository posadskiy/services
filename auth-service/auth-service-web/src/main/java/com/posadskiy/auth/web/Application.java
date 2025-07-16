package com.posadskiy.auth.web;

import io.micronaut.openapi.annotation.OpenAPIInclude;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info =
                @Info(
                        title = "Auth Service API",
                        version = "${project.version}",
                        description = "Authentication and Authorization Service API",
                        license = @License(name = "MIT", url = "https://opensource.org/license/mit"),
                        contact =
                                @Contact(
                                        url = "https://posadskiy.com",
                                        name = "Dimitri Posadskiy",
                                        email = "support@posadskiy.com")))
@OpenAPIInclude(
        classes = {
            io.micronaut.security.endpoints.LoginController.class,
            io.micronaut.security.endpoints.LogoutController.class
        })
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
