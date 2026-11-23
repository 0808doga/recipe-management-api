package com.dogasimsek.recipeapi.repository;

import com.dogasimsek.recipeapi.config.JpaAuditingConfig;
import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/** Repository tests with a real (in-memory) H2 database. */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class RecipeRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = entityManager.persist(new User("Owner", "owner@example.com", "hash", Role.USER));
        saveRecipe("Tomato Soup", "Warm soup", 30);
        saveRecipe("Pasta", "Quick dinner", 15);
        saveRecipe("Salad", "Fresh and light SOUP alternative", 10);
    }

    private Recipe saveRecipe(String title, String description, int preparationTime) {
        Recipe recipe = new Recipe();
        recipe.setOwner(owner);
        recipe.setTitle(title);
        recipe.setDescription(description);
        recipe.setInstructions("Cook");
        recipe.setPreparationTime(preparationTime);
        recipe.setServings(2);
        recipe.addIngredient(new Ingredient("Salt", 1.0, "pinch"));
        return recipeRepository.save(recipe);
    }

    @Test
    void searchFindsKeywordInTitleOrDescriptionIgnoringCase() {
        Page<Recipe> result = recipeRepository.searchByKeyword("soup", PageRequest.of(0, 10, Sort.by("title")));

        assertThat(result.getContent()).extracting(Recipe::getTitle).containsExactly("Salad", "Tomato Soup");
    }

    @Test
    void findByOwnerSupportsPagingAndSorting() {
        Page<Recipe> page = recipeRepository.findByOwner(owner,
                PageRequest.of(0, 2, Sort.by("preparationTime").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Recipe::getTitle).containsExactly("Salad", "Pasta");
    }

    @Test
    void savingRecipeCascadesToIngredientsAndFillsAuditFields() {
        Recipe recipe = saveRecipe("Bread", "Homemade", 90);
        entityManager.flush();

        assertThat(ingredientRepository.findByRecipeIdOrderByIdAsc(recipe.getId())).hasSize(1);
        assertThat(recipe.getCreatedAt()).isNotNull();
        assertThat(recipe.getCreatedBy()).isEqualTo("system");
    }

    @Test
    void deletingRecipeAlsoDeletesIngredients() {
        Recipe recipe = saveRecipe("Bread", "Homemade", 90);
        entityManager.flush();
        Long recipeId = recipe.getId();

        recipeRepository.delete(recipe);
        entityManager.flush();

        assertThat(ingredientRepository.findByRecipeIdOrderByIdAsc(recipeId)).isEmpty();
    }
}
