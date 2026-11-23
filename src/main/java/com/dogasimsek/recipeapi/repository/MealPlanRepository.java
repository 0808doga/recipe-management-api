package com.dogasimsek.recipeapi.repository;

import com.dogasimsek.recipeapi.entity.MealPlan;
import com.dogasimsek.recipeapi.entity.MealType;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    List<MealPlan> findByOwnerOrderByDateAscIdAsc(User owner);

    boolean existsByOwnerAndDateAndMealType(User owner, LocalDate date, MealType mealType);

    void deleteByRecipe(Recipe recipe);
}
