package com.smarthire.backend.features.auth.controller;

import com.smarthire.backend.features.auth.dto.AuthResponse;
import com.smarthire.backend.features.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Hidden
public class LegacyOAuthController {

    private final AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallbackLegacy(@org.springframework.web.bind.annotation.RequestParam("code") String code) {
        AuthResponse response = authService.githubLogin(code);
        
        // Return 302 Redirect to Frontend's callback handler
        String redirectUrl = frontendUrl + "/oauth/github-success" 
                + "?access_token=" + response.getAccessToken() 
                + "&refresh_token=" + response.getRefreshToken()
                + "&role=" + response.getRole()
                + "&onboarded=" + response.getIsOnboarded();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create(redirectUrl))
                .build();
    }
}
