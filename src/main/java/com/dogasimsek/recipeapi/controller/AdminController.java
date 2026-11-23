package com.dogasimsek.recipeapi.controller;

import com.dogasimsek.recipeapi.dto.AdminStatsResponse;
import com.dogasimsek.recipeapi.dto.ErrorResponse;
import com.dogasimsek.recipeapi.dto.RoleUpdateRequest;
import com.dogasimsek.recipeapi.dto.UserResponse;
import com.dogasimsek.recipeapi.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * ADMIN-only endpoints. Protected twice: in SecurityConfig (/api/admin/**) and with @PreAuthorize.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Logged in, but not an ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "All users")
    public List<UserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change the role of a user (ADMIN only)", description = "An admin cannot change their own role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role changed"),
            @ApiResponse(responseCode = "400", description = "Invalid role or trying to change own role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserResponse changeUserRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request,
                                       Principal principal) {
        return adminService.changeUserRole(id, request.role(), principal.getName());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get application statistics (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Number of users, recipes, ingredients and meal plans")
    public AdminStatsResponse getStatistics() {
        return adminService.getStatistics();
    }
}
