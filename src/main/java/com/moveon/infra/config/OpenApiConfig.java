package com.moveon.infra.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI moveonBotOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Moveon Bot API")
                        .description("AI Personal Assistant - REST API Documentation")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Moveon Bot Team")
                                .email("support@moveon.bot"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url(contextPath)
                                .description("Local development server")
                ));
    }
}
