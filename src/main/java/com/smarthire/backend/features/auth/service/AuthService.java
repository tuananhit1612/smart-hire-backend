package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.features.auth.dto.AuthResponse;
import com.smarthire.backend.features.auth.dto.LoginRequest;
import com.smarthire.backend.features.auth.dto.RefreshTokenRequest;
import com.smarthire.backend.features.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
