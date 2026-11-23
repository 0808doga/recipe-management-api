package com.dogasimsek.recipeapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngredientControllerIntegrationTest extends IntegrationTestSupport {

    private long addIngredient(String token, long recipeId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/recipes/" + recipeId + "/ingredients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", name, "quantity", 250, "unit", "G"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unit").value("g"))
                .andExpect(jsonPath("$.audit.createdBy").value("owner@example.com"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void ingredientCrudWorks() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Bread", 90);

        long ingredientId = addIngredient(token, recipeId, "Flour");

        mockMvc.perform(get("/api/recipes/" + recipeId + "/ingredients").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/recipes/" + recipeId + "/ingredients/" + ingredientId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Flour"));

        mockMvc.perform(put("/api/recipes/" + recipeId + "/ingredients/" + ingredientId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Whole wheat flour", "quantity", 300, "unit", "g"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Whole wheat flour"))
                .andExpect(jsonPath("$.quantity").value(300.0));

        mockMvc.perform(delete("/api/recipes/" + recipeId + "/ingredients/" + ingredientId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recipes/" + recipeId + "/ingredients/" + ingredientId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateIngredientNameReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Bread", 90);

        mockMvc.perform(post("/api/recipes/" + recipeId + "/ingredients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "SALT", "quantity", 1, "unit", "pinch"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ingredient 'SALT' already exists in this recipe"));
    }

    @Test
    void invalidQuantityReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Bread", 90);

        mockMvc.perform(post("/api/recipes/" + recipeId + "/ingredients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Egg", "quantity", 0, "unit", "piece"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").value("Quantity must be greater than 0"));
    }

    @Test
    void otherUserCannotChangeIngredients() throws Exception {
        String owner = registerAndLogin("owner@example.com");
        String other = registerAndLogin("other@example.com");
        long recipeId = createRecipe(owner, "Bread", 90);

        mockMvc.perform(post("/api/recipes/" + recipeId + "/ingredients")
                        .header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Sugar", "quantity", 1, "unit", "cup"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ingredientsOfMissingRecipeReturn404() throws Exception {
        String token = registerAndLogin("owner@example.com");

        mockMvc.perform(get("/api/recipes/9999/ingredients").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }
}
