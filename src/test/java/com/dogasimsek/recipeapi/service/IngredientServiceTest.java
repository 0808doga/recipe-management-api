package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.IngredientRequest;
import com.dogasimsek.recipeapi.dto.IngredientResponse;
import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.BusinessValidationException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private RecipeService recipeService;
    @Mock
    private UserService userService;

    @InjectMocks
    private IngredientService ingredientService;

    private User owner;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        owner = TestData.user(1L, "owner@example.com", Role.USER);
        recipe = TestData.recipe(10L, owner);
    }

    @Test
    void getIngredientsReturnsList() {
        Ingredient ingredient = TestData.ingredient(1L, "Flour", recipe);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(ingredientRepository.findByRecipeIdOrderByIdAsc(10L)).thenReturn(List.of(ingredient));

        List<IngredientResponse> result = ingredientService.getIngredients(10L);

        assertThat(result).extracting(IngredientResponse::name).containsExactly("Flour");
    }

    @Test
    void getIngredientsForMissingRecipeThrowsNotFound() {
        when(recipeService.findRecipe(99L)).thenThrow(new ResourceNotFoundException("Recipe with id 99 not found"));

        assertThatThrownBy(() -> ingredientService.getIngredients(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getIngredientReturnsIngredient() {
        Ingredient ingredient = TestData.ingredient(1L, "Flour", recipe);
        when(ingredientRepository.findByIdAndRecipeId(1L, 10L)).thenReturn(Optional.of(ingredient));

        assertThat(ingredientService.getIngredient(10L, 1L).recipeId()).isEqualTo(10L);
    }

    @Test
    void getIngredientNotFound() {
        when(ingredientRepository.findByIdAndRecipeId(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.getIngredient(10L, 5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ingredient with id 5 not found in recipe 10");
    }

    @Test
    void addIngredientSavesNormalizedIngredient() {
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(ingredientRepository.existsByRecipeIdAndNameIgnoreCase(10L, "Sugar")).thenReturn(false);
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IngredientResponse response = ingredientService.addIngredient(10L,
                new IngredientRequest(" Sugar ", 2.0, " TBSP "), "owner@example.com");

        assertThat(response.name()).isEqualTo("Sugar");
        assertThat(response.unit()).isEqualTo("tbsp");
        assertThat(recipe.getIngredients()).hasSize(1);
    }

    @Test
    void addDuplicateIngredientIsRejected() {
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(ingredientRepository.existsByRecipeIdAndNameIgnoreCase(10L, "Sugar")).thenReturn(true);

        assertThatThrownBy(() -> ingredientService.addIngredient(10L,
                new IngredientRequest("Sugar", 2.0, "g"), "owner@example.com"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("already exists");
        verify(ingredientRepository, never()).save(any());
    }

    @Test
    void addIngredientByNonOwnerIsForbidden() {
        User other = TestData.user(2L, "other@example.com", Role.USER);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(userService.getUserByEmail("other@example.com")).thenReturn(other);
        doThrow(new AccessDeniedException("You can only modify your own recipes"))
                .when(recipeService).checkCanModify(recipe, other);

        assertThatThrownBy(() -> ingredientService.addIngredient(10L,
                new IngredientRequest("Sugar", 2.0, "g"), "other@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateIngredientChangesValues() {
        Ingredient ingredient = TestData.ingredient(1L, "Flour", recipe);
        when(ingredientRepository.findByIdAndRecipeId(1L, 10L)).thenReturn(Optional.of(ingredient));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(ingredientRepository.existsByRecipeIdAndNameIgnoreCase(10L, "Whole wheat flour")).thenReturn(false);
        when(ingredientRepository.saveAndFlush(ingredient)).thenReturn(ingredient);

        IngredientResponse response = ingredientService.updateIngredient(10L, 1L,
                new IngredientRequest("Whole wheat flour", 300.0, "g"), "owner@example.com");

        assertThat(response.name()).isEqualTo("Whole wheat flour");
        assertThat(response.quantity()).isEqualTo(300.0);
    }

    @Test
    void updateIngredientToExistingNameIsRejected() {
        Ingredient ingredient = TestData.ingredient(1L, "Flour", recipe);
        when(ingredientRepository.findByIdAndRecipeId(1L, 10L)).thenReturn(Optional.of(ingredient));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(ingredientRepository.existsByRecipeIdAndNameIgnoreCase(10L, "Sugar")).thenReturn(true);

        assertThatThrownBy(() -> ingredientService.updateIngredient(10L, 1L,
                new IngredientRequest("Sugar", 1.0, "g"), "owner@example.com"))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void deleteIngredientRemovesItFromRecipe() {
        Ingredient ingredient = TestData.ingredient(1L, "Flour", recipe);
        when(ingredientRepository.findByIdAndRecipeId(1L, 10L)).thenReturn(Optional.of(ingredient));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        doCallRealMethod().when(recipeService).checkCanModify(recipe, owner);

        ingredientService.deleteIngredient(10L, 1L, "owner@example.com");

        assertThat(recipe.getIngredients()).isEmpty();
    }
}
