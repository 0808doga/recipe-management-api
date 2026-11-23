package com.dogasimsek.recipeapi.config;

import com.dogasimsek.recipeapi.entity.Ingredient;
import com.dogasimsek.recipeapi.entity.Recipe;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
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

/**
 * Creates sample data when the application starts in the "dev" profile:
 * one ADMIN account, one USER account and some recipes (useful for pagination demos).
 * Nothing is created if the data already exists.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String userEmail;
    private final String userPassword;

    public DataSeeder(UserRepository userRepository,
                      RecipeRepository recipeRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-email}") String adminEmail,
                      @Value("${app.seed.admin-password}") String adminPassword,
                      @Value("${app.seed.user-email}") String userEmail,
                      @Value("${app.seed.user-password}") String userPassword) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
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

        if (recipeRepository.count() == 0) {
            createSampleRecipes(user, admin);
            log.info("Sample recipes created");
        }
    }

    private User createUserIfMissing(String name, String email, String rawPassword, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating local {} account: {}", role, email);
            return userRepository.save(new User(name, email, passwordEncoder.encode(rawPassword), role));
        });
    }

    private void createSampleRecipes(User user, User admin) {
        saveRecipe(user, "Spaghetti Carbonara", "Classic Italian pasta with eggs and cheese",
                "Boil pasta. Fry pancetta. Mix eggs with cheese. Combine off the heat.", 25, 2,
                new Ingredient("Spaghetti", 200.0, "g"), new Ingredient("Egg", 2.0, "piece"),
                new Ingredient("Parmesan", 50.0, "g"));
        saveRecipe(user, "Menemen", "Turkish scrambled eggs with tomatoes and peppers",
                "Cook peppers in oil, add tomatoes, then add eggs and stir gently.", 15, 2,
                new Ingredient("Egg", 3.0, "piece"), new Ingredient("Tomato", 2.0, "piece"),
                new Ingredient("Green pepper", 2.0, "piece"));
        saveRecipe(user, "Lentil Soup", "Warm red lentil soup",
                "Cook onion and carrot, add lentils and water, boil 25 minutes, blend.", 40, 4,
                new Ingredient("Red lentils", 250.0, "g"), new Ingredient("Onion", 1.0, "piece"),
                new Ingredient("Water", 1.5, "l"));
        saveRecipe(user, "Greek Salad", "Fresh salad with feta cheese",
                "Chop vegetables, add olives and feta, dress with olive oil.", 10, 2,
                new Ingredient("Cucumber", 1.0, "piece"), new Ingredient("Feta", 100.0, "g"),
                new Ingredient("Olive oil", 2.0, "tbsp"));
        saveRecipe(user, "Pancakes", "Fluffy breakfast pancakes",
                "Mix flour, milk and eggs. Cook small portions in a hot pan.", 20, 4,
                new Ingredient("Flour", 200.0, "g"), new Ingredient("Milk", 300.0, "ml"),
                new Ingredient("Egg", 2.0, "piece"));
        saveRecipe(user, "Chicken Curry", "Mild chicken curry with rice",
                "Brown chicken, add onion, curry paste and coconut milk, simmer 20 minutes.", 45, 4,
                new Ingredient("Chicken breast", 500.0, "g"), new Ingredient("Coconut milk", 400.0, "ml"),
                new Ingredient("Curry paste", 2.0, "tbsp"));
        saveRecipe(user, "Guacamole", "Avocado dip",
                "Mash avocados, add lime juice, onion, salt and chopped tomato.", 10, 4,
                new Ingredient("Avocado", 3.0, "piece"), new Ingredient("Lime", 1.0, "piece"),
                new Ingredient("Salt", 1.0, "pinch"));
        saveRecipe(admin, "Banana Bread", "Sweet bread made with ripe bananas",
                "Mash bananas, mix with butter, sugar, eggs and flour, bake 60 minutes at 175C.", 75, 8,
                new Ingredient("Banana", 3.0, "piece"), new Ingredient("Flour", 250.0, "g"),
                new Ingredient("Sugar", 150.0, "g"));
        saveRecipe(admin, "Tomato Soup", "Simple creamy tomato soup",
                "Cook onion and garlic, add tomatoes and stock, simmer and blend.", 30, 4,
                new Ingredient("Tomato", 800.0, "g"), new Ingredient("Garlic", 2.0, "clove"),
                new Ingredient("Vegetable stock", 500.0, "ml"));
        saveRecipe(admin, "Omelette", "Quick cheese omelette",
                "Beat eggs, pour into a hot pan, add cheese and fold.", 8, 1,
                new Ingredient("Egg", 3.0, "piece"), new Ingredient("Cheese", 30.0, "g"));
        saveRecipe(admin, "Hummus", "Chickpea dip with tahini",
                "Blend chickpeas, tahini, lemon juice, garlic and olive oil until smooth.", 15, 6,
                new Ingredient("Chickpeas", 400.0, "g"), new Ingredient("Tahini", 3.0, "tbsp"),
                new Ingredient("Lemon juice", 2.0, "tbsp"));
        saveRecipe(admin, "Overnight Oats", "No-cook breakfast oats",
                "Mix oats, milk and yogurt in a jar. Keep in the fridge overnight.", 5, 1,
                new Ingredient("Oats", 50.0, "g"), new Ingredient("Milk", 120.0, "ml"),
                new Ingredient("Yogurt", 2.0, "tbsp"));
    }

    private void saveRecipe(User owner, String title, String description, String instructions,
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
        recipeRepository.save(recipe);
    }
}
