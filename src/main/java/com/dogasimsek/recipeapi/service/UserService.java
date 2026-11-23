package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.dto.UserResponse;
import com.dogasimsek.recipeapi.entity.User;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper service to find users. Used by the other services to load the logged-in user.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + email + "' not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return UserResponse.from(getUserByEmail(email));
    }
}
