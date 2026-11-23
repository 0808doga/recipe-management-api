package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.IngredientRequest;
import com.dogasimsek.recipeapi.dto.PageResponse;
import com.dogasimsek.recipeapi.dto.RecipeRequest;
import com.dogasimsek.recipeapi.dto.RecipeResponse;
import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.BusinessValidationException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import com.dogasimsek.recipeapi.repository.RecipeRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic for recipes.
 * Rules:
 * - every logged-in user can VIEW all recipes
 * - only the owner of a recipe or an ADMIN can UPDATE or DELETE it
 */
@Service
public class RecipeService {

    /** Only these fields can be used for sorting (protects against sorting by e.g. owner.password). */
    public static final List<String> SORTABLE_FIELDS =
            List.of("id", "title", "preparationTime", "servings", "createdAt", "updatedAt");

    private final RecipeRepository recipeRepository;
    private final MealPlanRepository mealPlanRepository;
    private final UserService userService;

    public RecipeService(RecipeRepository recipeRepository, MealPlanRepository mealPlanRepository,
                         UserService userService) {
        this.recipeRepository = recipeRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.userService = userService;
    }

    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, String currentUserEmail) {
        User owner = userService.getUserByEmail(currentUserEmail);
        Recipe recipe = new Recipe();
        recipe.setOwner(owner);
        copyRecipeFields(request, recipe);
        if (request.ingredients() != null) {
            replaceIngredients(recipe, request.ingredients());
        }
        // cascade = ALL on Recipe.ingredients also saves the ingredients
        return RecipeResponse.from(recipeRepository.save(recipe));
    }

    @Transactional(readOnly = true)
    public RecipeResponse getRecipeById(Long id) {
        return RecipeResponse.from(findRecipe(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> getAllRecipes(Pageable pageable) {
        validateSortFields(pageable);
        return PageResponse.from(recipeRepository.findAll(pageable).map(RecipeResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> searchRecipes(String keyword, Pageable pageable) {
        validateSortFields(pageable);
        return PageResponse.from(recipeRepository.searchByKeyword(keyword.trim(), pageable)
                .map(RecipeResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> getMyRecipes(String currentUserEmail, Pageable pageable) {
        validateSortFields(pageable);
        User owner = userService.getUserByEmail(currentUserEmail);
        return PageResponse.from(recipeRepository.findByOwner(owner, pageable).map(RecipeResponse::from));
    }

    @Transactional
    public RecipeResponse updateRecipe(Long id, RecipeRequest request, String currentUserEmail) {
        Recipe recipe = findRecipe(id);
        checkCanModify(recipe, userService.getUserByEmail(currentUserEmail));
        copyRecipeFields(request, recipe);
        if (request.ingredients() != null) {
            replaceIngredients(recipe, request.ingredients());
        }
        // saveAndFlush writes the change now, so updatedAt/lastModifiedBy are filled in the response
        return RecipeResponse.from(recipeRepository.saveAndFlush(recipe));
    }

    @Transactional
    public void deleteRecipe(Long id, String currentUserEmail) {
        Recipe recipe = findRecipe(id);
        checkCanModify(recipe, userService.getUserByEmail(currentUserEmail));
        // Meal plans that use this recipe cannot exist without it, so they are removed first.
        // Ingredients are removed automatically by cascade.
        mealPlanRepository.deleteByRecipe(recipe);
        recipeRepository.delete(recipe);
    }

    /** Loads a recipe or throws 404. Also used by IngredientService and MealPlanService. */
    public Recipe findRecipe(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe with id " + id + " not found"));
    }

    /** Business rule: only the owner or an ADMIN may change a recipe. */
    public void checkCanModify(Recipe recipe, User user) {
        boolean isOwner = recipe.getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only modify your own recipes");
        }
    }

    private void copyRecipeFields(RecipeRequest request, Recipe recipe) {
        recipe.setTitle(request.title().trim());
        recipe.setDescription(request.description() == null ? null : request.description().trim());
        recipe.setInstructions(request.instructions().trim());
        recipe.setPreparationTime(request.preparationTime());
        recipe.setServings(request.servings());
    }

    private void replaceIngredients(Recipe recipe, List<IngredientRequest> ingredientRequests) {
        checkNoDuplicateIngredientNames(ingredientRequests);
        // orphanRemoval deletes the old ingredients from the database
        recipe.getIngredients().clear();
        for (IngredientRequest ingredientRequest : ingredientRequests) {
            recipe.addIngredient(new Ingredient(ingredientRequest.name().trim(), ingredientRequest.quantity(),
                    ingredientRequest.unit().trim().toLowerCase()));
        }
    }

    /** Business rule: the same ingredient cannot appear twice in one recipe. */
    private void checkNoDuplicateIngredientNames(List<IngredientRequest> ingredientRequests) {
        Set<String> names = new HashSet<>();
        for (IngredientRequest ingredientRequest : ingredientRequests) {
            String name = ingredientRequest.name().trim().toLowerCase();
            if (!names.add(name)) {
                throw new BusinessValidationException(
                        "Ingredient '" + ingredientRequest.name().trim() + "' is listed more than once");
            }
        }
    }

    private void validateSortFields(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BusinessValidationException("Cannot sort by '" + order.getProperty()
                        + "'. Allowed sort fields: " + String.join(", ", SORTABLE_FIELDS));
            }
        }
    }
}
