package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.features.auth.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyResetToken(String token);

    void changePassword(ChangePasswordRequest request);

    AuthResponse githubLogin(String code);
}

