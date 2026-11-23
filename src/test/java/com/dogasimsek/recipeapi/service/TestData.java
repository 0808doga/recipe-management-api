package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;

/** Small helper to create test objects for the unit tests. */
final class TestData {

    private TestData() {
    }

    static User user(long id, String email, Role role) {
        User user = new User("User " + id, email, "hashed", role);
        user.setId(id);
        return user;
    }

    static Recipe recipe(long id, User owner) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setOwner(owner);
        recipe.setTitle("Recipe " + id);
        recipe.setDescription("Description");
        recipe.setInstructions("Cook it");
        recipe.setPreparationTime(20);
        recipe.setServings(2);
        return recipe;
    }

    static Ingredient ingredient(long id, String name, Recipe recipe) {
        Ingredient ingredient = new Ingredient(name, 100.0, "g");
        ingredient.setId(id);
        recipe.addIngredient(ingredient);
        return ingredient;
    }
}
