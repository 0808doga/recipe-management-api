package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.AdminStatsResponse;
import com.dogasimsek.recipeapi.dto.UserResponse;
import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.BusinessValidationException;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.IngredientRepository;
import com.dogasimsek.recipeapi.repository.MealPlanRepository;
import com.dogasimsek.recipeapi.repository.RecipeRepository;
import com.dogasimsek.recipeapi.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operations that only an ADMIN may use (the check is done in SecurityConfig and AdminController).
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final MealPlanRepository mealPlanRepository;

    public AdminService(UserRepository userRepository, RecipeRepository recipeRepository,
                        IngredientRepository ingredientRepository, MealPlanRepository mealPlanRepository) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.mealPlanRepository = mealPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse changeUserRole(Long userId, Role newRole, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));
        // Business rule: an admin cannot remove their own admin rights by mistake
        if (user.getEmail().equals(currentAdminEmail)) {
            throw new BusinessValidationException("You cannot change your own role");
        }
        user.setRole(newRole);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse getStatistics() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.ADMIN),
                recipeRepository.count(),
                ingredientRepository.count(),
                mealPlanRepository.count());
    }
}
