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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private MealPlanRepository mealPlanRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllUsersReturnsUsersWithoutPasswords() {
        when(userRepository.findAll(Sort.by("id")))
                .thenReturn(List.of(TestData.user(1L, "a@example.com", Role.ADMIN),
                        TestData.user(2L, "b@example.com", Role.USER)));

        List<UserResponse> users = adminService.getAllUsers();

        assertThat(users).extracting(UserResponse::email).containsExactly("a@example.com", "b@example.com");
    }

    @Test
    void changeUserRoleUpdatesRole() {
        User user = TestData.user(2L, "b@example.com", Role.USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = adminService.changeUserRole(2L, Role.ADMIN, "admin@example.com");

        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void adminCannotChangeOwnRole() {
        User admin = TestData.user(1L, "admin@example.com", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.changeUserRole(1L, Role.USER, "admin@example.com"))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessage("You cannot change your own role");
    }

    @Test
    void changeRoleOfMissingUserThrowsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.changeUserRole(9L, Role.ADMIN, "admin@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getStatisticsCountsEverything() {
        when(userRepository.count()).thenReturn(3L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(recipeRepository.count()).thenReturn(10L);
        when(ingredientRepository.count()).thenReturn(30L);
        when(mealPlanRepository.count()).thenReturn(4L);

        AdminStatsResponse stats = adminService.getStatistics();

        assertThat(stats).isEqualTo(new AdminStatsResponse(3, 1, 10, 30, 4));
    }
}
