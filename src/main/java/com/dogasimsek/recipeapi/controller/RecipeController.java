package com.dogasimsek.recipeapi.controller;

import com.dogasimsek.recipeapi.dto.ErrorResponse;
import com.dogasimsek.recipeapi.dto.PageResponse;
import com.dogasimsek.recipeapi.dto.RecipeRequest;
import com.dogasimsek.recipeapi.dto.RecipeResponse;
import com.dogasimsek.recipeapi.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipes")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class RecipeController {

    private static final String PAGINATION_HELP = "Pagination: page (starts at 0), size (max 50). "
            + "Sorting: sort=field,asc|desc. Use several sort parameters to sort by multiple fields, "
            + "e.g. sort=preparationTime,asc and sort=title,asc. "
            + "Allowed fields: id, title, preparationTime, servings, createdAt, updatedAt.";

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    @Operation(summary = "List all recipes (paginated)", description = PAGINATION_HELP)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "One page of recipes"),
            @ApiResponse(responseCode = "400", description = "Invalid sort field",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<RecipeResponse> getAllRecipes(
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return recipeService.getAllRecipes(pageable);
    }

    @GetMapping("/search")
    @Operation(summary = "Search recipes by keyword (paginated)",
            description = "Finds recipes whose title or description contains the keyword. " + PAGINATION_HELP)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching recipes"),
            @ApiResponse(responseCode = "400", description = "Keyword missing or invalid sort field",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public PageResponse<RecipeResponse> searchRecipes(
            @Parameter(description = "Text to search for", example = "soup")
            @RequestParam @NotBlank(message = "Keyword must not be empty")
            @Size(max = 100, message = "Keyword cannot be longer than 100 characters") String keyword,
            @ParameterObject @PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return recipeService.searchRecipes(keyword, pageable);
    }

    @GetMapping("/my")
    @Operation(summary = "List my own recipes (paginated)", description = PAGINATION_HELP)
    @ApiResponse(responseCode = "200", description = "Recipes created by the logged-in user")
    public PageResponse<RecipeResponse> getMyRecipes(
            Principal principal,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return recipeService.getMyRecipes(principal.getName(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one recipe with its ingredients and audit information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe found"),
            @ApiResponse(responseCode = "404", description = "Recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RecipeResponse getRecipe(@PathVariable Long id) {
        return recipeService.getRecipeById(id);
    }

    @PostMapping
    @Operation(summary = "Create a recipe", description = "Ingredients can be sent in the same request (optional).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recipe created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RecipeResponse> createRecipe(@Valid @RequestBody RecipeRequest request,
                                                       Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recipeService.createRecipe(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a recipe (owner or ADMIN only)",
            description = "If 'ingredients' is sent, the old ingredient list is replaced. If it is left out, ingredients stay the same.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recipe updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not the owner of the recipe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public RecipeResponse updateRecipe(@PathVariable Long id, @Valid @RequestBody RecipeRequest request,
                                       Principal principal) {
        return recipeService.updateRecipe(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recipe (owner or ADMIN only)",
            description = "Its ingredients and the meal plans that use it are deleted too.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recipe deleted"),
            @ApiResponse(responseCode = "403", description = "Not the owner of the recipe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id, Principal principal) {
        recipeService.deleteRecipe(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
