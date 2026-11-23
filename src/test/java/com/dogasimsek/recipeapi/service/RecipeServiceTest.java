package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.IngredientRequest;
import com.dogasimsek.recipeapi.dto.PageResponse;
import com.dogasimsek.recipeapi.dto.RecipeRequest;
import com.dogasimsek.recipeapi.dto.RecipeResponse;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.BusinessValidationException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import com.dogasimsek.recipeapi.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private MealPlanRepository mealPlanRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private RecipeService recipeService;

    private User owner;
    private User otherUser;
    private User admin;

    @BeforeEach
    void setUp() {
        owner = TestData.user(1L, "owner@example.com", Role.USER);
        otherUser = TestData.user(2L, "other@example.com", Role.USER);
        admin = TestData.user(3L, "admin@example.com", Role.ADMIN);
    }

    private RecipeRequest request(List<IngredientRequest> ingredients) {
        return new RecipeRequest(" Pasta ", "Tasty", "Boil water", 15, 2, ingredients);
    }

    @Test
    void createRecipeSavesRecipeWithIngredients() {
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponse response = recipeService.createRecipe(request(List.of(
                new IngredientRequest("Pasta", 200.0, "G"),
                new IngredientRequest("Salt", 1.0, "pinch"))), "owner@example.com");

        assertThat(response.title()).isEqualTo("Pasta");
        assertThat(response.ownerId()).isEqualTo(1L);
        assertThat(response.ingredients()).hasSize(2);
        assertThat(response.ingredients().get(0).unit()).isEqualTo("g");
    }

    @Test
    void createRecipeWithoutIngredientsWorks() {
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponse response = recipeService.createRecipe(request(null), "owner@example.com");

        assertThat(response.ingredients()).isEmpty();
    }

    @Test
    void createRecipeWithDuplicateIngredientNamesIsRejected() {
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);

        assertThatThrownBy(() -> recipeService.createRecipe(request(List.of(
                new IngredientRequest("Egg", 2.0, "piece"),
                new IngredientRequest("egg", 1.0, "piece"))), "owner@example.com"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("more than once");
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void getRecipeByIdReturnsRecipe() {
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(TestData.recipe(5L, owner)));

        assertThat(recipeService.getRecipeById(5L).id()).isEqualTo(5L);
    }

    @Test
    void getRecipeByIdThrowsNotFound() {
        when(recipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getRecipeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Recipe with id 99 not found");
    }

    @Test
    void getAllRecipesReturnsPageInformation() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("title").ascending().and(Sort.by("servings")));
        List<Recipe> recipes = List.of(TestData.recipe(1L, owner), TestData.recipe(2L, owner));
        when(recipeRepository.findAll(pageable)).thenReturn(new PageImpl<>(recipes, pageable, 5));

        PageResponse<RecipeResponse> page = recipeService.getAllRecipes(pageable);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
    }

    @Test
    void getAllRecipesRejectsUnknownSortField() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("password"));

        assertThatThrownBy(() -> recipeService.getAllRecipes(pageable))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Cannot sort by 'password'");
    }

    @Test
    void searchRecipesTrimsKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        when(recipeRepository.searchByKeyword("soup", pageable))
                .thenReturn(new PageImpl<>(List.of(TestData.recipe(1L, owner)), pageable, 1));

        assertThat(recipeService.searchRecipes("  soup ", pageable).totalElements()).isEqualTo(1);
    }

    @Test
    void getMyRecipesUsesCurrentUser() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeRepository.findByOwner(owner, pageable))
                .thenReturn(new PageImpl<>(List.of(TestData.recipe(1L, owner)), pageable, 1));

        assertThat(recipeService.getMyRecipes("owner@example.com", pageable).content()).hasSize(1);
    }

    @Test
    void ownerCanUpdateRecipeAndReplaceIngredients() {
        Recipe recipe = TestData.recipe(5L, owner);
        TestData.ingredient(1L, "Old", recipe);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeRepository.saveAndFlush(recipe)).thenReturn(recipe);

        RecipeResponse response = recipeService.updateRecipe(5L,
                request(List.of(new IngredientRequest("New", 1.0, "cup"))), "owner@example.com");

        assertThat(response.title()).isEqualTo("Pasta");
        assertThat(response.ingredients()).extracting("name").containsExactly("New");
    }

    @Test
    void updateWithoutIngredientsKeepsExistingIngredients() {
        Recipe recipe = TestData.recipe(5L, owner);
        TestData.ingredient(1L, "Old", recipe);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeRepository.saveAndFlush(recipe)).thenReturn(recipe);

        RecipeResponse response = recipeService.updateRecipe(5L, request(null), "owner@example.com");

        assertThat(response.ingredients()).extracting("name").containsExactly("Old");
    }

    @Test
    void otherUserCannotUpdateRecipe() {
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(TestData.recipe(5L, owner)));
        when(userService.getUserByEmail("other@example.com")).thenReturn(otherUser);

        assertThatThrownBy(() -> recipeService.updateRecipe(5L, request(null), "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only modify your own recipes");
    }

    @Test
    void adminCanUpdateAnyRecipe() {
        Recipe recipe = TestData.recipe(5L, owner);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(userService.getUserByEmail("admin@example.com")).thenReturn(admin);
        when(recipeRepository.saveAndFlush(recipe)).thenReturn(recipe);

        assertThat(recipeService.updateRecipe(5L, request(null), "admin@example.com").title()).isEqualTo("Pasta");
    }

    @Test
    void ownerCanDeleteRecipeAndItsMealPlansAreRemoved() {
        Recipe recipe = TestData.recipe(5L, owner);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);

        recipeService.deleteRecipe(5L, "owner@example.com");

        verify(mealPlanRepository).deleteByRecipe(recipe);
        verify(recipeRepository).delete(recipe);
    }

    @Test
    void otherUserCannotDeleteRecipe() {
        Recipe recipe = TestData.recipe(5L, owner);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));
        when(userService.getUserByEmail("other@example.com")).thenReturn(otherUser);

        assertThatThrownBy(() -> recipeService.deleteRecipe(5L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);
        verify(recipeRepository, never()).delete(any());
    }
}
