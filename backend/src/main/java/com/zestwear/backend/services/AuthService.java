package com.zestwear.backend.services;

import com.zestwear.backend.config.JwtUtil;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public User getUserFromAuth(String auth) {
        // Prefer SecurityContext if available
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.zestwear.backend.models.User) {
                return (com.zestwear.backend.models.User) principal;
            }
            if (principal instanceof com.zestwear.backend.config.CustomUserDetails) {
                return ((com.zestwear.backend.config.CustomUserDetails) principal).getUser();
            }
        }

        // Fallback to parsing Authorization header (backwards compatibility)
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    return userRepository.findById(userId).orElse(null);
                }
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public boolean isAdmin(User user) {
        return user != null && "Admin".equals(user.getRole());
    }

    public boolean isStaffOrAdmin(User user) {
        return user != null && ("Admin".equals(user.getRole()) || "Staff".equals(user.getRole()));
    }
}
