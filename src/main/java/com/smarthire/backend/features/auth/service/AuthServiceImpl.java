package com.smarthire.backend.features.auth.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.security.JwtUtil;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.auth.dto.*;
import com.smarthire.backend.features.auth.entity.PasswordResetToken;
import com.smarthire.backend.features.auth.entity.RefreshToken;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.PasswordResetTokenRepository;
import com.smarthire.backend.features.auth.repository.RefreshTokenRepository;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.shared.enums.Role;
import com.smarthire.backend.shared.service.EmailService;
import com.smarthire.backend.shared.service.EmailTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Role role = parseRole(request.getRole());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid or revoked refresh token"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setIsRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BadRequestException("Refresh token expired");
        }

        // Revoke old token
        storedToken.setIsRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = createRefreshToken(user);

        return buildAuthResponse(user, accessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(request.getRefreshToken())
                .orElse(null);

        if (storedToken != null) {
            storedToken.setIsRevoked(true);
            refreshTokenRepository.save(storedToken);
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        // Hủy tất cả token cũ
        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        // Tạo token mới, hết hạn sau 15 phút
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Gửi email chứa link reset password
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String userName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String htmlContent = EmailTemplates.buildResetPasswordEmail(userName, resetLink);
        emailService.sendHtmlEmail(user.getEmail(), "[SmartHire] Đặt lại mật khẩu", htmlContent);

        log.info("Password reset email sent to {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndIsUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or already used reset token"));

        if (resetToken.isExpired()) {
            resetToken.setIsUsed(true);
            passwordResetTokenRepository.save(resetToken);
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        // Đánh dấu token đã dùng
        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke tất cả refresh token (force re-login)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = SecurityUtils.getCurrentUser();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke tất cả refresh token (force re-login)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        long expirationMs = refreshTokenExpiration;

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(expirationMs / 1000))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private Role parseRole(String roleStr) {
        try {
            Role role = Role.valueOf(roleStr.toUpperCase());
            if (role == Role.ADMIN) {
                throw new BadRequestException("Cannot register as ADMIN");
            }
            return role;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role. Must be CANDIDATE or HR");
        }
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}

