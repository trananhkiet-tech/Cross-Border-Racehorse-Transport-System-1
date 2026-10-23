package com.example.racetransport.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI tại /swagger-ui.html; bấm Authorize và dán accessToken. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI irtmsOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("IRTMS API")
                        .version("1.0.0")
                        .description("International Racehorse Transportation Management System – REST API. "
                                + "Thanh toán chỉ là mô phỏng; dữ liệu tuân thủ là dữ liệu demo."))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme().name(scheme).type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
