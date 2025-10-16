package com.bookingsystem.configuration;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.url}")
    private String url;

    @Bean
    public OpenAPI bookingSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking System API")
                        .description("REST API documentation for the Booking System application")
                )
                .addServersItem(new Server()
                        .url(url)
                        .description("Local server")
                );
    }
}
