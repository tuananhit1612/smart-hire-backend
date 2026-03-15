package com.smarthire.backend.core.security;

import com.smarthire.backend.core.exception.UnauthorizedException;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.shared.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Lấy CustomUserDetails của user hiện tại từ SecurityContext.
     * @throws UnauthorizedException nếu chưa xác thực
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    /**
     * Lấy User entity của user hiện tại.
     */
    public static User getCurrentUser() {
        return getCurrentUserDetails().getUser();
    }

    /**
     * Lấy ID của user hiện tại.
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Lấy email của user hiện tại.
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    /**
     * Lấy Role enum của user hiện tại.
     */
    public static Role getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    /**
     * Kiểm tra user hiện tại có role cụ thể không.
     */
    public static boolean hasRole(Role role) {
        return getCurrentUserRole() == role;
    }
}
