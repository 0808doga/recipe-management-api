package com.dogasimsek.recipeapi.config;

import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.MealPlan;
import com.dogasimsek.recipeapi.entity.MealType;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import com.dogasimsek.recipeapi.repository.RecipeRepository;
import com.dogasimsek.recipeapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Creates sample data when the application starts in the "dev" profile:
 * - 2 accounts: one ADMIN and one USER
 * - 5 recipes (Pasta, Pizza, Salad, Omelette, Soup), each with 3 ingredients
 * - 3 meal plans for the USER account
 * The data is only created when the recipes table is empty,
 * so restarting the application never creates duplicates.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final MealPlanRepository mealPlanRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String userEmail;
    private final String userPassword;

    public DataSeeder(UserRepository userRepository,
                      RecipeRepository recipeRepository,
                      MealPlanRepository mealPlanRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-email}") String adminEmail,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.user-email}") String userEmail,
                      @Value("${app.seed.user-password}") String userPassword) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.userEmail = userEmail;
        this.userPassword = userPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        User admin = createUserIfMissing("Demo Admin", adminEmail, adminPassword, Role.ADMIN);
        User user = createUserIfMissing("Demo User", userEmail, userPassword, Role.USER);

        // Only add sample data to an empty database (no duplicates after a restart)
        if (recipeRepository.count() > 0) {
            log.info("Sample data already exists, nothing created");
            return;
        }

        // Recipes 1-3 belong to the USER, recipes 4-5 belong to the ADMIN
        Recipe pasta = saveRecipe(user, "Pasta", "Simple pasta with tomato sauce",
                "1. Boil the pasta for 10 minutes. 2. Warm the tomato sauce. 3. Mix the pasta with the sauce.",
                20, 2,
                new Ingredient("Pasta", 200.0, "g"),
                new Ingredient("Tomato sauce", 250.0, "ml"),
                new Ingredient("Cheese", 50.0, "g"));
        saveRecipe(user, "Pizza", "Homemade cheese pizza",
                "1. Roll out the dough. 2. Add tomato sauce and cheese. 3. Bake for 15 minutes at 220 C.",
                35, 4,
                new Ingredient("Pizza dough", 500.0, "g"),
                new Ingredient("Tomato sauce", 150.0, "ml"),
                new Ingredient("Mozzarella", 200.0, "g"));
        Recipe salad = saveRecipe(user, "Salad", "Fresh green salad",
                "1. Wash and cut the vegetables. 2. Put them in a bowl. 3. Add olive oil and mix.",
                10, 2,
                new Ingredient("Lettuce", 1.0, "piece"),
                new Ingredient("Tomato", 2.0, "piece"),
                new Ingredient("Olive oil", 2.0, "tbsp"));
        Recipe omelette = saveRecipe(admin, "Omelette", "Quick egg omelette",
                "1. Beat the eggs with salt. 2. Pour them into a hot pan with butter. 3. Cook for 3 minutes and fold.",
                10, 1,
                new Ingredient("Egg", 3.0, "piece"),
                new Ingredient("Butter", 10.0, "g"),
                new Ingredient("Salt", 1.0, "pinch"));
        saveRecipe(admin, "Soup", "Warm vegetable soup",
                "1. Cut the vegetables. 2. Boil them in water for 25 minutes. 3. Blend and serve.",
                40, 4,
                new Ingredient("Potato", 2.0, "piece"),
                new Ingredient("Carrot", 2.0, "piece"),
                new Ingredient("Water", 1.0, "l"));

        // 3 meal plans for the USER (tomorrow and the day after)
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        saveMealPlan(user, omelette, tomorrow, MealType.BREAKFAST);
        saveMealPlan(user, salad, tomorrow, MealType.LUNCH);
        saveMealPlan(user, pasta, tomorrow.plusDays(1), MealType.DINNER);

        log.info("Sample data created: 5 recipes and 3 meal plans");
    }

    private User createUserIfMissing(String name, String email, String rawPassword, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating local {} account: {}", role, email);
            return userRepository.save(new User(name, email, passwordEncoder.encode(rawPassword), role));
        });
    }

    private Recipe saveRecipe(User owner, String title, String description, String instructions,
                              int preparationTime, int servings, Ingredient... ingredients) {
        Recipe recipe = new Recipe();
        recipe.setOwner(owner);
        recipe.setTitle(title);
        recipe.setDescription(description);
        recipe.setInstructions(instructions);
        recipe.setPreparationTime(preparationTime);
        recipe.setServings(servings);
        for (Ingredient ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        return recipeRepository.save(recipe);
    }

    private void saveMealPlan(User owner, Recipe recipe, LocalDate date, MealType mealType) {
        MealPlan mealPlan = new MealPlan();
        mealPlan.setOwner(owner);
        mealPlan.setRecipe(recipe);
        mealPlan.setDate(date);
        mealPlan.setMealType(mealType);
        mealPlanRepository.save(mealPlan);
    }
}
