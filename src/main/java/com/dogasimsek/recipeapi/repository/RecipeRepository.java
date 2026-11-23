package com.dogasimsek.recipeapi.repository;

import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Page<Recipe> findByOwner(User owner, Pageable pageable);

    /** Finds recipes whose title OR description contains the keyword (case-insensitive). */
    @Query("SELECT r FROM Recipe r "
            + "WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Recipe> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
