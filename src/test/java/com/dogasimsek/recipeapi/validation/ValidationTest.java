package com.dogasimsek.recipeapi.validation;

import com.dogasimsek.recipeapi.dto.IngredientRequest;
import com.dogasimsek.recipeapi.dto.MealPlanRequest;
import com.dogasimsek.recipeapi.dto.RecipeRequest;
import com.dogasimsek.recipeapi.dto.RegisterRequest;
import com.dogasimsek.recipeapi.entity.MealType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests the Bean Validation rules and the two custom validators directly (no Spring needed). */
class ValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static <T> Map<String, String> errors(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        return violations.stream().collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (first, second) -> first));
    }

    @Test
    void validIngredientHasNoErrors() {
        assertThat(errors(new IngredientRequest("Flour", 250.5, "g"))).isEmpty();
        assertThat(errors(new IngredientRequest("Egg", 2.0, "Piece"))).isEmpty();
    }

    @Test
    void unknownUnitIsRejected() {
        assertThat(errors(new IngredientRequest("Flour", 1.0, "bucket")))
                .containsEntry("unit", "Unit must be one of: g, kg, ml, l, tsp, tbsp, cup, piece, pinch, slice, clove");
    }

    @Test
    void fractionalPiecesAreRejected() {
        assertThat(errors(new IngredientRequest("Egg", 2.5, "piece")))
                .containsEntry("quantity", "Quantity must be a whole number for countable units (piece, slice, clove)");
    }

    @Test
    void zeroOrNegativeQuantityIsRejected() {
        assertThat(errors(new IngredientRequest("Flour", 0.0, "g")))
                .containsEntry("quantity", "Quantity must be greater than 0");
        assertThat(errors(new IngredientRequest("Flour", 20000.0, "g")))
                .containsEntry("quantity", "Quantity cannot be more than 10000");
    }

    @Test
    void invalidPreparationInformationIsRejected() {
        RecipeRequest request = new RecipeRequest("Ok title", null, "Cook", 0, 0, null);

        assertThat(errors(request))
                .containsEntry("preparationTime", "Preparation time must be at least 1 minute")
                .containsEntry("servings", "Servings must be at least 1");
        assertThat(errors(new RecipeRequest("Ok title", null, "Cook", 2000, 2, null)))
                .containsEntry("preparationTime", "Preparation time cannot be more than 1440 minutes (24 hours)");
    }

    @Test
    void nestedIngredientsAreValidated() {
        RecipeRequest request = new RecipeRequest("Pasta", null, "Cook", 10, 2,
                List.of(new IngredientRequest("Egg", 1.5, "piece")));

        assertThat(errors(request)).containsKey("ingredients[0].quantity");
    }

    @Test
    void pastMealPlanDateIsRejected() {
        MealPlanRequest request = new MealPlanRequest(LocalDate.now().minusDays(1), MealType.LUNCH, 1L);

        assertThat(errors(request)).containsEntry("date", "Meal plan date cannot be in the past");
    }

    @Test
    void weakPasswordAndBadEmailAreRejected() {
        Map<String, String> result = errors(new RegisterRequest("Doga", "not-an-email", "onlyletters"));

        assertThat(result).containsEntry("email", "Email must be a valid email address")
                .containsEntry("password", "Password must contain at least one letter and one number");
    }
}
