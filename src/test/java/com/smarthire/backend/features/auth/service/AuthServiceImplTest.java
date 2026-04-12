package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.security.JwtUtil;
import com.smarthire.backend.features.auth.dto.AuthResponse;
import com.smarthire.backend.features.auth.dto.RegisterRequest;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.PasswordResetTokenRepository;
import com.smarthire.backend.features.auth.repository.RefreshTokenRepository;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.shared.enums.Role;
import com.smarthire.backend.shared.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void register_ShouldCreateUserAndReturnTokens_WhenValidRequest() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test User");
        request.setEmail("test@ex.com");
        request.setPassword("Password123!");
        request.setRole("CANDIDATE");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pwd");
        
        User savedUser = User.builder()
                .id(1L)
                .email("test@ex.com")
                .role(Role.CANDIDATE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access_token_123");
        when(refreshTokenRepository.save(any())).thenReturn(null);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("access_token_123", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("CANDIDATE", response.getRole());
        assertEquals(1L, response.getUserId());

        verify(userRepository).existsByEmail("test@ex.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateAccessToken(1L, "test@ex.com", "CANDIDATE");
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@ex.com");
        when(userRepository.existsByEmail("test@ex.com")).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> authService.register(request));
        assertEquals("Email already exists", ex.getMessage());
        
        verify(userRepository).existsByEmail("test@ex.com");
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }
}
