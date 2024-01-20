package io.zenwave360.example.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerUI {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI() //
            .info(new Info() //
                .title("Generated OpenAPI")
                .description("Complete Generated OpenAPI for all Modules")
                .version("v0.0.1"));
    }

}
