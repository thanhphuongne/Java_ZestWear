package com.zestwear.backend.services;

import com.zestwear.backend.config.JwtUtil;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public User getUserFromAuth(String auth) {
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
