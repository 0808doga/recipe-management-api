package com.dogasimsek.recipeapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void normalUserGets403OnAdminEndpoints() throws Exception {
        String userToken = registerAndLogin("user@example.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource."));
        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousGets401OnAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListUsersAndSeeStatistics() throws Exception {
        String userToken = registerAndLogin("user@example.com");
        String adminToken = createAdminAndLogin("admin@example.com");
        createRecipe(userToken, "Pasta", 15);

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].password").doesNotExist());

        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalAdmins").value(1))
                .andExpect(jsonPath("$.totalRecipes").value(1))
                .andExpect(jsonPath("$.totalIngredients").value(1));
    }

    @Test
    void adminCanPromoteUserButNotChangeOwnRole() throws Exception {
        registerAndLogin("user@example.com");
        String adminToken = createAdminAndLogin("admin@example.com");
        long userId = userRepository.findByEmail("user@example.com").orElseThrow().getId();
        long adminId = userRepository.findByEmail("admin@example.com").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + userId + "/role").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(put("/api/admin/users/" + adminId + "/role").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("role", "USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You cannot change your own role"));
    }

    @Test
    void promotedUserCanUseAdminEndpoints() throws Exception {
        String userToken = registerAndLogin("user@example.com");
        String adminToken = createAdminAndLogin("admin@example.com");
        long userId = userRepository.findByEmail("user@example.com").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/users/" + userId + "/role").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(toJson(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk());

        // the role is loaded from the database on every request, so the same token now works
        mockMvc.perform(get("/api/admin/stats").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());
    }
}
