package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.IngredientRequest;
import com.dogasimsek.recipeapi.dto.IngredientResponse;
import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.exception.BusinessValidationException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for the ingredients of a recipe.
 * Only the recipe owner or an ADMIN can add, change or delete ingredients.
 */
@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final RecipeService recipeService;
    private final UserService userService;

    public IngredientService(IngredientRepository ingredientRepository, RecipeService recipeService,
                             UserService userService) {
        this.ingredientRepository = ingredientRepository;
        this.recipeService = recipeService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getIngredients(Long recipeId) {
        recipeService.findRecipe(recipeId); // 404 if the recipe does not exist
        return ingredientRepository.findByRecipeIdOrderByIdAsc(recipeId).stream()
                .map(IngredientResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IngredientResponse getIngredient(Long recipeId, Long ingredientId) {
        return IngredientResponse.from(findIngredient(recipeId, ingredientId));
    }

    @Transactional
    public IngredientResponse addIngredient(Long recipeId, IngredientRequest request, String currentUserEmail) {
        Recipe recipe = recipeService.findRecipe(recipeId);
        recipeService.checkCanModify(recipe, userService.getUserByEmail(currentUserEmail));

        String name = request.name().trim();
        if (ingredientRepository.existsByRecipeIdAndNameIgnoreCase(recipeId, name)) {
            throw new BusinessValidationException("Ingredient '" + name + "' already exists in this recipe");
        }
        Ingredient ingredient = new Ingredient(name, request.quantity(), request.unit().trim().toLowerCase());
        recipe.addIngredient(ingredient);
        return IngredientResponse.from(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientResponse updateIngredient(Long recipeId, Long ingredientId, IngredientRequest request,
                                               String currentUserEmail) {
        Ingredient ingredient = findIngredient(recipeId, ingredientId);
        recipeService.checkCanModify(ingredient.getRecipe(), userService.getUserByEmail(currentUserEmail));

        String newName = request.name().trim();
        boolean nameChanged = !ingredient.getName().equalsIgnoreCase(newName);
        if (nameChanged && ingredientRepository.existsByRecipeIdAndNameIgnoreCase(recipeId, newName)) {
            throw new BusinessValidationException("Ingredient '" + newName + "' already exists in this recipe");
        }
        ingredient.setName(newName);
        ingredient.setQuantity(request.quantity());
        ingredient.setUnit(request.unit().trim().toLowerCase());
        return IngredientResponse.from(ingredientRepository.saveAndFlush(ingredient));
    }

    @Transactional
    public void deleteIngredient(Long recipeId, Long ingredientId, String currentUserEmail) {
        Ingredient ingredient = findIngredient(recipeId, ingredientId);
        Recipe recipe = ingredient.getRecipe();
        recipeService.checkCanModify(recipe, userService.getUserByEmail(currentUserEmail));
        // orphanRemoval on Recipe.ingredients deletes the row
        recipe.removeIngredient(ingredient);
    }

    private Ingredient findIngredient(Long recipeId, Long ingredientId) {
        return ingredientRepository.findByIdAndRecipeId(ingredientId, recipeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ingredient with id " + ingredientId + " not found in recipe " + recipeId));
    }
}
