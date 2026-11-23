package com.dogasimsek.recipeapi.repository;

import com.dogasimsek.recipeapi.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByRecipeIdOrderByIdAsc(Long recipeId);

    Optional<Ingredient> findByIdAndRecipeId(Long id, Long recipeId);

    boolean existsByRecipeIdAndNameIgnoreCase(Long recipeId, String name);
}
