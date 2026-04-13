package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.UserRepository;
import com.zestwear.backend.services.AuthService;
import com.zestwear.backend.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthService authService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String fullName = body.getOrDefault("fullName", email);

        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Email already exists", null));
        }

        User user = new User(email, passwordEncoder.encode(password), fullName);
        user.setRole("User");
        userRepository.save(user);

        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user);
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registered", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        User user = userRepository.findByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(new ApiResponse<>(false, "Invalid credentials", null));
        }
        String refreshToken = UUID.randomUUID().toString();
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user);
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged in", data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null) return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Missing refreshToken", null));
        User user = userRepository.findByRefreshToken(refreshToken);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Invalid refresh token", null));
        String accessToken = jwtUtil.generateToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        return ResponseEntity.ok(new ApiResponse<>(true, "Refreshed", data));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user != null) {
            user.setRefreshToken(null);
            userRepository.save(user);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user != null) {
            user.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(true, null, user));
        }
        return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Map<String, String> body) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        if (body.containsKey("fullName")) user.setFullName(body.get("fullName"));
        userRepository.save(user);
        user.setPassword(null);
        return ResponseEntity.ok(new ApiResponse<>(true, "Updated", user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Map<String, String> body) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        String current = body.get("currentPassword");
        String next = body.get("newPassword");
        if (!passwordEncoder.matches(current, user.getPassword())) {
            return ResponseEntity.status(400).body(new ApiResponse<>(false, "Current password incorrect", null));
        }
        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password changed", null));
    }

    @PostMapping(value = "/avatar", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestHeader(value = "Authorization", required = false) String auth, @RequestPart("file") MultipartFile file) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        try {
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            String ext = "";
            int i = filename.lastIndexOf('.');
            if (i >= 0) ext = filename.substring(i);
            String newName = UUID.randomUUID().toString() + ext;
            Path uploadDir = Paths.get("src/main/resources/static/avatars");
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(newName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/avatars/" + newName;
            user.setAvatarUrl(url);
            userRepository.save(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "Uploaded", url));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email);
        if (user == null) return ResponseEntity.ok(new ApiResponse<>(true, "If the email exists we'll send reset instructions", null));
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        // In real app send email. Here return token for convenience in dev
        Map<String, Object> data = new HashMap<>();
        data.put("resetToken", token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reset token generated", data));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("resetToken");
        String newPassword = body.get("newPassword");
        if (token == null || newPassword == null) return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Missing fields", null));
        User user = userRepository.findAll().stream().filter(u -> token.equals(u.getResetToken())).findFirst().orElse(null);
        if (user == null || user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(400).body(new ApiResponse<>(false, "Invalid or expired token", null));
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset", null));
    }
}
