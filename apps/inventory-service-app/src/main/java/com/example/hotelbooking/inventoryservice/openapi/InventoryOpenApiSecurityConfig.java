package com.example.hotelbooking.inventoryservice.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Inventory Service API",
            version = "v0.5.2",
            description =
                """
            Inventory service API.

            Public catalog endpoints are available without authentication.
            Administrative endpoints require bearer authentication in security-enabled profiles.

            The target service-to-service model for booking-to-inventory gRPC communication is mTLS.
            """))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER)
public class InventoryOpenApiSecurityConfig {}
