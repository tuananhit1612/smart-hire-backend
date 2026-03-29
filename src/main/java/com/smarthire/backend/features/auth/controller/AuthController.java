package com.smarthire.backend.features.auth.controller;

import com.smarthire.backend.features.auth.dto.*;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.auth.service.AuthService;
import com.smarthire.backend.shared.constants.ApiPaths;
import com.smarthire.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping(ApiPaths.AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh token, logout APIs")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new CANDIDATE or HR account and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user by email/password and returns JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token", description = "Issues new access + refresh tokens using a valid refresh token (rotation)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the given refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Sends a password reset link to the user's email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent.", null));
    }

    @GetMapping("/verify-reset-token")
    @Operation(summary = "Verify reset token", description = "Checks if a reset password token is valid, used, or expired")
    public ResponseEntity<ApiResponse<Void>> verifyResetToken(
            @org.springframework.web.bind.annotation.RequestParam String token) {
        authService.verifyResetToken(token);
        return ResponseEntity.ok(ApiResponse.success("Reset token is valid.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Please login with new password.", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns full profile of the authenticated user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole().name(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "isActive", user.getIsActive(),
                "createdAt", user.getCreatedAt().toString(),
                "updatedAt", user.getUpdatedAt().toString(),
                "isOnboarded", user.getIsOnboarded() != null ? user.getIsOnboarded() : false
        );
        return ResponseEntity.ok(ApiResponse.success("Current user", userData));
    }

    /**
     * GitHub OAuth callback
     * GitHub redirects here after user authorization:
     *   GET /api/auth/github/callback?code=xxx
     * We exchange the code for JWT tokens and redirect the browser
     * to the Next.js frontend callback page.
     */
    @GetMapping("/github/callback")
    @Operation(summary = "GitHub OAuth callback", description = "Handles GitHub redirect, issues JWT, redirects to frontend")
    public RedirectView githubCallback(@RequestParam String code) {
        try {
            AuthResponse auth = authService.githubLogin(code);
            String redirect = frontendUrl + "/auth/callback"
                    + "?accessToken=" + URLEncoder.encode(auth.getAccessToken(), StandardCharsets.UTF_8)
                    + "&refreshToken=" + URLEncoder.encode(auth.getRefreshToken(), StandardCharsets.UTF_8)
                    + "&role=" + URLEncoder.encode(auth.getRole(), StandardCharsets.UTF_8)
                    + "&isOnboarded=" + (auth.getIsOnboarded() != null ? auth.getIsOnboarded() : false);
            return new RedirectView(redirect);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "GitHub login failed";
            return new RedirectView(frontendUrl + "/login?error=github_failed&message="
                    + URLEncoder.encode(msg, StandardCharsets.UTF_8));
        }
    }
}
