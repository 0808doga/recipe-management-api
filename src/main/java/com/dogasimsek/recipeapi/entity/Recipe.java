package com.dogasimsek.recipeapi.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * A cooking recipe.
 * - Many recipes belong to one user (owner)  -> @ManyToOne
 * - One recipe has many ingredients           -> @OneToMany
 * createdAt / updatedAt / createdBy / lastModifiedBy come from AuditableEntity.
 */
@Entity
@Table(name = "recipes", indexes = {
        @Index(name = "idx_recipe_title", columnList = "title"),
        @Index(name = "idx_recipe_owner", columnList = "owner_id")
})
public class Recipe extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 5000)
    private String instructions;

    // in minutes
    @Column(nullable = false)
    private Integer preparationTime;

    @Column(nullable = false)
    private Integer servings;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recipe_owner"))
    private User owner;

    // cascade ALL: saving/deleting a recipe also saves/deletes its ingredients.
    // orphanRemoval: an ingredient removed from this list is deleted from the database.
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Ingredient> ingredients = new ArrayList<>();

    public Recipe() {
    }

    /** Adds an ingredient and keeps both sides of the relationship in sync. */
    public void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }

    /** Removes an ingredient; orphanRemoval deletes it from the database. */
    public void removeIngredient(Ingredient ingredient) {
        ingredients.remove(ingredient);
        ingredient.setRecipe(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Integer getPreparationTime() {
        return preparationTime;
    }

    public void setPreparationTime(Integer preparationTime) {
        this.preparationTime = preparationTime;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }
}
