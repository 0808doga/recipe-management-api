package com.dogasimsek.recipeapi.config;

import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.repository.IngredientRepository;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import com.dogasimsek.recipeapi.repository.RecipeRepository;
import com.dogasimsek.recipeapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the app with the "dev" profile (so DataSeeder runs) but with its own in-memory database.
 */
@SpringBootTest
@ActiveProfiles({"dev", "test"})
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:seeder-test;DB_CLOSE_DELAY=-1")
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private IngredientRepository ingredientRepository;
    @Autowired
    private MealPlanRepository mealPlanRepository;

    @Test
    void createsSampleDataOnlyOnce() {
        // DataSeeder already ran once at startup. Run it again, like after a restart.
        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(recipeRepository.findAll()).extracting(Recipe::getTitle)
                .containsExactlyInAnyOrder("Pasta", "Pizza", "Salad", "Omelette", "Soup");
        assertThat(ingredientRepository.count()).isEqualTo(15);
        assertThat(mealPlanRepository.count()).isEqualTo(3);
    }
}
