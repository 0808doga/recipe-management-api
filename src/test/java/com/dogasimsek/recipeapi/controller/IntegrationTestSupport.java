package com.dogasimsek.recipeapi.controller;

import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.repository.IngredientRepository;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import com.dogasimsek.recipeapi.repository.RecipeRepository;
import com.dogasimsek.recipeapi.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for the integration tests: starts the full application with the "test" profile
 * (in-memory H2), cleans the database before each test and offers small helper methods.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "Password123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RecipeRepository recipeRepository;
    @Autowired
    protected IngredientRepository ingredientRepository;
    @Autowired
    protected MealPlanRepository mealPlanRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        mealPlanRepository.deleteAll();
        ingredientRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Registers a USER through the API and returns its access token. */
    protected String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Test User", "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
        return login(email);
    }

    /** Creates an ADMIN directly in the database (admins cannot self-register) and returns its token. */
    protected String createAdminAndLogin(String email) throws Exception {
        userRepository.save(new User("Admin", email, passwordEncoder.encode(PASSWORD), Role.ADMIN));
        return login(email);
    }

    protected String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).get("accessToken").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** Creates a recipe through the API and returns its id. */
    protected long createRecipe(String token, String title, int preparationTime) throws Exception {
        Map<String, Object> body = Map.of(
                "title", title,
                "description", "Description of " + title,
                "instructions", "Cook " + title,
                "preparationTime", preparationTime,
                "servings", 2,
                "ingredients", java.util.List.of(Map.of("name", "Salt", "quantity", 1, "unit", "pinch")));
        MvcResult result = mockMvc.perform(post("/api/recipes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }
}
