package com.dogasimsek.recipeapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MealPlanControllerIntegrationTest extends IntegrationTestSupport {

    private static final String TOMORROW = LocalDate.now().plusDays(1).toString();

    private Map<String, Object> body(String date, String mealType, long recipeId) {
        return Map.of("date", date, "mealType", mealType, "recipeId", recipeId);
    }

    private long createMealPlan(String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void mealPlanCrudWorks() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Pasta", 15);

        long id = createMealPlan(token, body(TOMORROW, "DINNER", recipeId));

        mockMvc.perform(get("/api/meal-plans").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recipeTitle").value("Pasta"));

        mockMvc.perform(get("/api/meal-plans/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealType").value("DINNER"))
                .andExpect(jsonPath("$.audit.createdBy").value("owner@example.com"));

        mockMvc.perform(put("/api/meal-plans/" + id).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body(TOMORROW, "LUNCH", recipeId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealType").value("LUNCH"));

        mockMvc.perform(delete("/api/meal-plans/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/meal-plans/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameDateAndMealTypeTwiceReturns409() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Pasta", 15);
        createMealPlan(token, body(TOMORROW, "DINNER", recipeId));

        mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body(TOMORROW, "DINNER", recipeId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a DINNER planned on " + TOMORROW));
    }

    @Test
    void pastDateReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Pasta", 15);

        mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body(LocalDate.now().minusDays(1).toString(), "DINNER", recipeId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.date").value("Meal plan date cannot be in the past"));
    }

    @Test
    void unknownMealTypeReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Pasta", 15);

        mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body(TOMORROW, "BRUNCH", recipeId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Invalid value 'BRUNCH' for field 'mealType'. Allowed values: [BREAKFAST, LUNCH, DINNER, SNACK]"));
    }

    @Test
    void wrongDateFormatReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long recipeId = createRecipe(token, "Pasta", 15);

        mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body("01/12/2026", "LUNCH", recipeId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("yyyy-MM-dd")));
    }

    @Test
    void missingRecipeReturns404() throws Exception {
        String token = registerAndLogin("owner@example.com");

        mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body(TOMORROW, "LUNCH", 9999))))
                .andExpect(status().isNotFound());
    }

    @Test
    void mealPlansArePrivate() throws Exception {
        String owner = registerAndLogin("owner@example.com");
        String other = registerAndLogin("other@example.com");
        long recipeId = createRecipe(owner, "Pasta", 15);
        long id = createMealPlan(owner, body(TOMORROW, "DINNER", recipeId));

        mockMvc.perform(get("/api/meal-plans/" + id).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only access your own meal plans"));
        mockMvc.perform(delete("/api/meal-plans/" + id).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/meal-plans").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
