package com.smarthire.backend.core.config;

import com.smarthire.backend.core.security.JwtAuthenticationFilter;
import com.smarthire.backend.shared.constants.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints ──
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(
                    ApiPaths.AUTH + "/register",
                    ApiPaths.AUTH + "/login",
                    ApiPaths.AUTH + "/refresh-token"
                ).permitAll()

                // ── Swagger / OpenAPI ──
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // ── Public job listing (browsable without login) ──
                .requestMatchers(HttpMethod.GET, ApiPaths.JOBS, ApiPaths.JOBS + "/**").permitAll()

                // ── Admin-only endpoints ──
                .requestMatchers(ApiPaths.ADMIN + "/**").hasRole("ADMIN")

                // ── HR + Admin endpoints ──
                .requestMatchers(HttpMethod.POST, ApiPaths.JOBS).hasAnyRole("HR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, ApiPaths.JOBS + "/**").hasAnyRole("HR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, ApiPaths.JOBS + "/**").hasAnyRole("HR", "ADMIN")
                .requestMatchers(ApiPaths.COMPANIES + "/**").hasAnyRole("HR", "ADMIN")
                .requestMatchers(ApiPaths.DASHBOARD + "/**").hasAnyRole("HR", "ADMIN")

                // ── Everything else requires authentication ──
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS ──

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── 401 Unauthorized handler (not logged in / invalid token) ──

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication required. Please provide a valid token.\"}");
        };
    }

    // ── 403 Forbidden handler (logged in but insufficient role) ──

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"You do not have permission to access this resource.\"}");
        };
    }

    // ── Beans ──

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
