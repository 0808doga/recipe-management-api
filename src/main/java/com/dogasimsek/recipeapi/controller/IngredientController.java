package com.dogasimsek.recipeapi.controller;

import com.dogasimsek.recipeapi.dto.ErrorResponse;
import com.dogasimsek.recipeapi.dto.IngredientRequest;
import com.dogasimsek.recipeapi.dto.IngredientResponse;
import com.dogasimsek.recipeapi.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/recipes/{recipeId}/ingredients")
@Tag(name = "Ingredients")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recipe or ingredient not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    @Operation(summary = "List the ingredients of a recipe")
    @ApiResponse(responseCode = "200", description = "List of ingredients")
    public List<IngredientResponse> getIngredients(@PathVariable Long recipeId) {
        return ingredientService.getIngredients(recipeId);
    }

    @GetMapping("/{ingredientId}")
    @Operation(summary = "Get one ingredient")
    @ApiResponse(responseCode = "200", description = "Ingredient found")
    public IngredientResponse getIngredient(@PathVariable Long recipeId, @PathVariable Long ingredientId) {
        return ingredientService.getIngredient(recipeId, ingredientId);
    }

    @PostMapping
    @Operation(summary = "Add an ingredient to a recipe (owner or ADMIN only)",
            description = "Units: g, kg, ml, l, tsp, tbsp, cup, piece, pinch, slice, clove. "
                    + "For piece/slice/clove the quantity must be a whole number.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ingredient added"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate ingredient name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner of the recipe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IngredientResponse> addIngredient(@PathVariable Long recipeId,
                                                            @Valid @RequestBody IngredientRequest request,
                                                            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ingredientService.addIngredient(recipeId, request, principal.getName()));
    }

    @PutMapping("/{ingredientId}")
    @Operation(summary = "Update an ingredient (owner or ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ingredient updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner of the recipe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public IngredientResponse updateIngredient(@PathVariable Long recipeId, @PathVariable Long ingredientId,
                                               @Valid @RequestBody IngredientRequest request,
                                               Principal principal) {
        return ingredientService.updateIngredient(recipeId, ingredientId, request, principal.getName());
    }

    @DeleteMapping("/{ingredientId}")
    @Operation(summary = "Delete an ingredient (owner or ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ingredient deleted"),
            @ApiResponse(responseCode = "403", description = "Not the owner of the recipe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long recipeId, @PathVariable Long ingredientId,
                                                 Principal principal) {
        ingredientService.deleteIngredient(recipeId, ingredientId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
