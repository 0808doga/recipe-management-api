package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.MealPlanRequest;
import com.dogasimsek.recipeapi.dto.MealPlanResponse;
import com.dogasimsek.recipeapi.entity.MealPlan;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.DuplicateResourceException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for meal plans.
 * Meal plans are PRIVATE: a user can only see and change their own meal plans.
 */
@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final RecipeService recipeService;
    private final UserService userService;

    public MealPlanService(MealPlanRepository mealPlanRepository, RecipeService recipeService,
                           UserService userService) {
        this.mealPlanRepository = mealPlanRepository;
        this.recipeService = recipeService;
        this.userService = userService;
    }

    @Transactional
    public MealPlanResponse createMealPlan(MealPlanRequest request, String currentUserEmail) {
        User owner = userService.getUserByEmail(currentUserEmail);
        Recipe recipe = recipeService.findRecipe(request.recipeId());
        checkSlotIsFree(owner, request);

        MealPlan mealPlan = new MealPlan();
        mealPlan.setOwner(owner);
        mealPlan.setRecipe(recipe);
        mealPlan.setDate(request.date());
        mealPlan.setMealType(request.mealType());
        return MealPlanResponse.from(mealPlanRepository.save(mealPlan));
    }

    @Transactional(readOnly = true)
    public List<MealPlanResponse> getMyMealPlans(String currentUserEmail) {
        User owner = userService.getUserByEmail(currentUserEmail);
        return mealPlanRepository.findByOwnerOrderByDateAscIdAsc(owner).stream()
                .map(MealPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MealPlanResponse getMealPlan(Long id, String currentUserEmail) {
        return MealPlanResponse.from(findOwnMealPlan(id, currentUserEmail));
    }

    @Transactional
    public MealPlanResponse updateMealPlan(Long id, MealPlanRequest request, String currentUserEmail) {
        MealPlan mealPlan = findOwnMealPlan(id, currentUserEmail);
        Recipe recipe = recipeService.findRecipe(request.recipeId());

        boolean slotChanged = !mealPlan.getDate().equals(request.date())
                || mealPlan.getMealType() != request.mealType();
        if (slotChanged) {
            checkSlotIsFree(mealPlan.getOwner(), request);
        }
        mealPlan.setRecipe(recipe);
        mealPlan.setDate(request.date());
        mealPlan.setMealType(request.mealType());
        return MealPlanResponse.from(mealPlanRepository.saveAndFlush(mealPlan));
    }

    @Transactional
    public void deleteMealPlan(Long id, String currentUserEmail) {
        mealPlanRepository.delete(findOwnMealPlan(id, currentUserEmail));
    }

    /** Loads a meal plan and makes sure it belongs to the current user. */
    private MealPlan findOwnMealPlan(Long id, String currentUserEmail) {
        MealPlan mealPlan = mealPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meal plan with id " + id + " not found"));
        User currentUser = userService.getUserByEmail(currentUserEmail);
        if (!mealPlan.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only access your own meal plans");
        }
        return mealPlan;
    }

    /** Business rule: only one meal plan per user, date and meal type. */
    private void checkSlotIsFree(User owner, MealPlanRequest request) {
        if (mealPlanRepository.existsByOwnerAndDateAndMealType(owner, request.date(), request.mealType())) {
            throw new DuplicateResourceException("You already have a " + request.mealType()
                    + " planned on " + request.date());
        }
    }
}
