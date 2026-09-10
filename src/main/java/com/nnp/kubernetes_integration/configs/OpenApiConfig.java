package com.nnp.kubernetes_integration.configs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger documentation configuration for Kubernetes Integration Service.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI kubernetesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PICC-PP-Kubernetes-Integration REST API")
                        .description("Enterprise REST API integration and interactive terminal microservice for Kubernetes. "
                                + "Provides real-time pod metrics, cluster resource management, deployment lifecycle orchestration, "
                                + "and interactive WebSocket TTY pod terminals for the Nubo Native Platform (NNP).")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Nubo Native Platform Team")
                                .email("contribution@nubons.com")
                                .url("https://github.com/Nubo-Native-Platform/PICC-PP-Kubernetes-Integration"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current Server Context"),
                        new Server().url("http://localhost:" + serverPort).description("Local Development Server")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token for authorized requests")));
    }
}
