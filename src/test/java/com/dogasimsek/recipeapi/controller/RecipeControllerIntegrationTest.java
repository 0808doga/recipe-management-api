package com.dogasimsek.recipeapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecipeControllerIntegrationTest extends IntegrationTestSupport {

    private Map<String, Object> recipeBody(String title, int preparationTime) {
        return Map.of("title", title, "description", "Tasty", "instructions", "Cook it",
                "preparationTime", preparationTime, "servings", 2);
    }

    @Test
    void createRecipeReturns201WithIngredientsAndAudit() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Map<String, Object> body = Map.of("title", "Spaghetti Carbonara", "instructions", "Boil and mix",
                "preparationTime", 25, "servings", 2,
                "ingredients", List.of(Map.of("name", "Spaghetti", "quantity", 200, "unit", "g"),
                        Map.of("name", "Egg", "quantity", 2, "unit", "piece")));

        mockMvc.perform(post("/api/recipes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.ingredients", hasSize(2)))
                .andExpect(jsonPath("$.audit.createdBy").value("owner@example.com"))
                .andExpect(jsonPath("$.audit.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.audit.updatedAt").isNotEmpty());
    }

    @Test
    void getRecipeById() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long id = createRecipe(token, "Lentil Soup", 40);

        mockMvc.perform(get("/api/recipes/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lentil Soup"))
                .andExpect(jsonPath("$.ingredients[0].name").value("Salt"));
    }

    @Test
    void missingRecipeReturns404() throws Exception {
        String token = registerAndLogin("owner@example.com");

        mockMvc.perform(get("/api/recipes/9999").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recipe with id 9999 not found"));
    }

    @Test
    void invalidIdTypeReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");

        mockMvc.perform(get("/api/recipes/abc").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listRecipesWithPaginationAndMultipleSortFields() throws Exception {
        String token = registerAndLogin("owner@example.com");
        createRecipe(token, "Banana Bread", 60);
        createRecipe(token, "Apple Pie", 60);
        createRecipe(token, "Omelette", 10);

        mockMvc.perform(get("/api/recipes").header("Authorization", bearer(token))
                        .param("page", "0").param("size", "2")
                        .param("sort", "preparationTime,desc").param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Apple Pie"))
                .andExpect(jsonPath("$.content[1].title").value("Banana Bread"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/recipes").header("Authorization", bearer(token))
                        .param("page", "1").param("size", "2").param("sort", "preparationTime,desc"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Omelette"))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void sortingByNotAllowedFieldReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");

        mockMvc.perform(get("/api/recipes").header("Authorization", bearer(token)).param("sort", "owner,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Cannot sort by 'owner'")));
    }

    @Test
    void searchAndMyRecipes() throws Exception {
        String owner = registerAndLogin("owner@example.com");
        String other = registerAndLogin("other@example.com");
        createRecipe(owner, "Tomato Soup", 30);
        createRecipe(other, "Lentil Soup", 40);
        createRecipe(owner, "Pasta", 15);

        mockMvc.perform(get("/api/recipes/search").header("Authorization", bearer(owner)).param("keyword", "SOUP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/recipes/my").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void blankSearchKeywordReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");

        mockMvc.perform(get("/api/recipes/search").header("Authorization", bearer(token)).param("keyword", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.keyword").value("Keyword must not be empty"));

        mockMvc.perform(get("/api/recipes/search").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'keyword' is missing"));
    }

    @Test
    void invalidRecipeReturnsAllValidationErrors() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Map<String, Object> body = Map.of("title", "", "instructions", "Cook", "preparationTime", 0, "servings", 2,
                "ingredients", List.of(Map.of("name", "Egg", "quantity", 1.5, "unit", "piece"),
                        Map.of("name", "Water", "quantity", -1, "unit", "bucket")));

        mockMvc.perform(post("/api/recipes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.preparationTime")
                        .value("Preparation time must be at least 1 minute"))
                .andExpect(jsonPath("$.validationErrors['ingredients[0].quantity']")
                        .value("Quantity must be a whole number for countable units (piece, slice, clove)"))
                .andExpect(jsonPath("$.validationErrors['ingredients[1].quantity']")
                        .value("Quantity must be greater than 0"))
                .andExpect(jsonPath("$.validationErrors['ingredients[1].unit']").exists());
    }

    @Test
    void duplicateIngredientNamesInRecipeReturns400() throws Exception {
        String token = registerAndLogin("owner@example.com");
        Map<String, Object> body = Map.of("title", "Double Egg", "instructions", "Cook", "preparationTime", 5,
                "servings", 1, "ingredients", List.of(Map.of("name", "Egg", "quantity", 1, "unit", "piece"),
                        Map.of("name", "egg", "quantity", 2, "unit", "piece")));

        mockMvc.perform(post("/api/recipes").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ingredient 'egg' is listed more than once"));
    }

    @Test
    void ownerCanUpdateRecipeAndAuditShowsModifier() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long id = createRecipe(token, "Pasta", 15);

        mockMvc.perform(put("/api/recipes/" + id).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(recipeBody("Better Pasta", 20))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Better Pasta"))
                .andExpect(jsonPath("$.preparationTime").value(20))
                .andExpect(jsonPath("$.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.audit.lastModifiedBy").value("owner@example.com"));
    }

    @Test
    void otherUserCannotUpdateOrDeleteRecipe() throws Exception {
        String owner = registerAndLogin("owner@example.com");
        String other = registerAndLogin("other@example.com");
        long id = createRecipe(owner, "Pasta", 15);

        mockMvc.perform(put("/api/recipes/" + id).header("Authorization", bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(recipeBody("Hacked", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only modify your own recipes"));

        mockMvc.perform(delete("/api/recipes/" + id).header("Authorization", bearer(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateAnyRecipe() throws Exception {
        String owner = registerAndLogin("owner@example.com");
        String admin = createAdminAndLogin("admin@example.com");
        long id = createRecipe(owner, "Pasta", 15);

        mockMvc.perform(put("/api/recipes/" + id).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(recipeBody("Moderated", 15))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audit.createdBy").value("owner@example.com"))
                .andExpect(jsonPath("$.audit.lastModifiedBy").value("admin@example.com"));
    }

    @Test
    void deleteRecipeAlsoDeletesItsMealPlans() throws Exception {
        String token = registerAndLogin("owner@example.com");
        long id = createRecipe(token, "Pasta", 15);
        mockMvc.perform(post("/api/meal-plans").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("date", java.time.LocalDate.now().plusDays(1).toString(),
                                "mealType", "DINNER", "recipeId", id))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/recipes/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/recipes/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/meal-plans").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
