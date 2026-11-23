package com.dogasimsek.recipeapi.service;

import com.dogasimsek.recipeapi.entity.Role;
import com.dogasimsek.recipeapi.exception.ResourceNotFoundException;
import com.dogasimsek.recipeapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getProfileReturnsUser() {
        when(userRepository.findByEmail("a@example.com"))
                .thenReturn(Optional.of(TestData.user(1L, "a@example.com", Role.USER)));

        assertThat(userService.getProfile("a@example.com").role()).isEqualTo(Role.USER);
    }

    @Test
    void getUserByEmailThrowsWhenMissing() {
        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("x@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
