package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.MealPlanRequest;
import com.dogasimsek.recipeapi.dto.MealPlanResponse;
import com.dogasimsek.recipeapi.entity.MealPlan;
import com.dogasimsek.recipeapi.entity.MealType;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.DuplicateResourceException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Mock
    private MealPlanRepository mealPlanRepository;
    @Mock
    private RecipeService recipeService;
    @Mock
    private UserService userService;

    @InjectMocks
    private MealPlanService mealPlanService;

    private User owner;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        owner = TestData.user(1L, "owner@example.com", Role.USER);
        recipe = TestData.recipe(10L, owner);
    }

    private MealPlan mealPlan(long id, User planOwner) {
        MealPlan mealPlan = new MealPlan();
        mealPlan.setId(id);
        mealPlan.setOwner(planOwner);
        mealPlan.setRecipe(recipe);
        mealPlan.setDate(TOMORROW);
        mealPlan.setMealType(MealType.LUNCH);
        return mealPlan;
    }

    @Test
    void createMealPlanSavesIt() {
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(mealPlanRepository.existsByOwnerAndDateAndMealType(owner, TOMORROW, MealType.DINNER)).thenReturn(false);
        when(mealPlanRepository.save(any(MealPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MealPlanResponse response = mealPlanService.createMealPlan(
                new MealPlanRequest(TOMORROW, MealType.DINNER, 10L), "owner@example.com");

        assertThat(response.mealType()).isEqualTo(MealType.DINNER);
        assertThat(response.recipeTitle()).isEqualTo("Recipe 10");
        assertThat(response.ownerId()).isEqualTo(1L);
    }

    @Test
    void createMealPlanInTakenSlotThrowsDuplicate() {
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(mealPlanRepository.existsByOwnerAndDateAndMealType(owner, TOMORROW, MealType.DINNER)).thenReturn(true);

        assertThatThrownBy(() -> mealPlanService.createMealPlan(
                new MealPlanRequest(TOMORROW, MealType.DINNER, 10L), "owner@example.com"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already have a DINNER");
        verify(mealPlanRepository, never()).save(any());
    }

    @Test
    void getMyMealPlansReturnsOnlyOwnerPlans() {
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(mealPlanRepository.findByOwnerOrderByDateAscIdAsc(owner)).thenReturn(List.of(mealPlan(1L, owner)));

        assertThat(mealPlanService.getMyMealPlans("owner@example.com")).hasSize(1);
    }

    @Test
    void getMealPlanOfOwnerWorks() {
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(mealPlan(1L, owner)));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);

        assertThat(mealPlanService.getMealPlan(1L, "owner@example.com").id()).isEqualTo(1L);
    }

    @Test
    void getMealPlanOfAnotherUserIsForbidden() {
        User other = TestData.user(2L, "other@example.com", Role.USER);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(mealPlan(1L, owner)));
        when(userService.getUserByEmail("other@example.com")).thenReturn(other);

        assertThatThrownBy(() -> mealPlanService.getMealPlan(1L, "other@example.com"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only access your own meal plans");
    }

    @Test
    void getMissingMealPlanThrowsNotFound() {
        when(mealPlanRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealPlanService.getMealPlan(9L, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMealPlanToFreeSlotWorks() {
        MealPlan existing = mealPlan(1L, owner);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(mealPlanRepository.existsByOwnerAndDateAndMealType(owner, TOMORROW, MealType.BREAKFAST)).thenReturn(false);
        when(mealPlanRepository.saveAndFlush(existing)).thenReturn(existing);

        MealPlanResponse response = mealPlanService.updateMealPlan(1L,
                new MealPlanRequest(TOMORROW, MealType.BREAKFAST, 10L), "owner@example.com");

        assertThat(response.mealType()).isEqualTo(MealType.BREAKFAST);
    }

    @Test
    void updateMealPlanWithSameSlotDoesNotCheckDuplicate() {
        MealPlan existing = mealPlan(1L, owner);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(mealPlanRepository.saveAndFlush(existing)).thenReturn(existing);

        mealPlanService.updateMealPlan(1L, new MealPlanRequest(TOMORROW, MealType.LUNCH, 10L), "owner@example.com");

        verify(mealPlanRepository, never()).existsByOwnerAndDateAndMealType(any(), any(), any());
    }

    @Test
    void updateMealPlanToTakenSlotThrowsDuplicate() {
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(mealPlan(1L, owner)));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);
        when(recipeService.findRecipe(10L)).thenReturn(recipe);
        when(mealPlanRepository.existsByOwnerAndDateAndMealType(owner, TOMORROW, MealType.DINNER)).thenReturn(true);

        assertThatThrownBy(() -> mealPlanService.updateMealPlan(1L,
                new MealPlanRequest(TOMORROW, MealType.DINNER, 10L), "owner@example.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteMealPlanDeletesIt() {
        MealPlan existing = mealPlan(1L, owner);
        when(mealPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userService.getUserByEmail("owner@example.com")).thenReturn(owner);

        mealPlanService.deleteMealPlan(1L, "owner@example.com");

        verify(mealPlanRepository).delete(existing);
    }
}
