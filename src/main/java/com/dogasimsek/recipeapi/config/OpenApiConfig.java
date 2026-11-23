package com.dogasimsek.recipeapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI settings. Swagger UI: http://localhost:8080/swagger-ui.html
 * The "bearerAuth" scheme adds the "Authorize" button for the JWT.
 */
@Configuration
@SecurityScheme(
        name = OpenApiConfig.SECURITY_SCHEME_NAME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the accessToken from POST /api/auth/login (without the word 'Bearer').")
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI recipeManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recipe Management API")
                        .version("1.0.0")
                        .description("REST API for recipes, ingredients and meal plans. "
                                + "1) Register or use a test account, 2) call /api/auth/login, "
                                + "3) click 'Authorize' and paste the accessToken.")
                        .contact(new Contact().name("Doga Simsek - Student ID 34489")))
                // every endpoint needs the JWT unless it says otherwise
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register, log in, refresh token"),
                        new Tag().name("Recipes").description("Recipe CRUD, search, pagination and sorting"),
                        new Tag().name("Ingredients").description("Manage the ingredients of a recipe"),
                        new Tag().name("Meal Plans").description("Private meal plans of the logged-in user"),
                        new Tag().name("Admin").description("ADMIN-only operations")));
    }
}
