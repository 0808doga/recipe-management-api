package com.dogasimsek.recipeapi.controller;

import com.dogasimsek.recipeapi.dto.ErrorResponse;
import com.dogasimsek.recipeapi.dto.MealPlanRequest;
import com.dogasimsek.recipeapi.dto.MealPlanResponse;
import com.dogasimsek.recipeapi.service.MealPlanService;
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
@RequestMapping("/api/meal-plans")
@Tag(name = "Meal Plans")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @GetMapping
    @Operation(summary = "List my meal plans", description = "Only the meal plans of the logged-in user, sorted by date.")
    @ApiResponse(responseCode = "200", description = "List of meal plans")
    public List<MealPlanResponse> getMyMealPlans(Principal principal) {
        return mealPlanService.getMyMealPlans(principal.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my meal plans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meal plan found"),
            @ApiResponse(responseCode = "403", description = "Meal plan belongs to another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Meal plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public MealPlanResponse getMealPlan(@PathVariable Long id, Principal principal) {
        return mealPlanService.getMealPlan(id, principal.getName());
    }

    @PostMapping
    @Operation(summary = "Create a meal plan",
            description = "mealType: BREAKFAST, LUNCH, DINNER or SNACK. Date must be today or later. "
                    + "Only one meal plan per date and meal type is allowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Meal plan created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "This date and meal type is already planned",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MealPlanResponse> createMealPlan(@Valid @RequestBody MealPlanRequest request,
                                                           Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealPlanService.createMealPlan(request, principal.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update one of my meal plans")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Meal plan updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Meal plan belongs to another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Meal plan or recipe not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "This date and meal type is already planned",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public MealPlanResponse updateMealPlan(@PathVariable Long id, @Valid @RequestBody MealPlanRequest request,
                                           Principal principal) {
        return mealPlanService.updateMealPlan(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete one of my meal plans")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Meal plan deleted"),
            @ApiResponse(responseCode = "403", description = "Meal plan belongs to another user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Meal plan not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMealPlan(@PathVariable Long id, Principal principal) {
        mealPlanService.deleteMealPlan(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
