package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.UserRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
public class UsersController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UsersController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = authService.getUserFromAuth(auth);
        if (!authService.isAdmin(me)) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(new ApiResponse<>(true, null, users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> get(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        User me = authService.getUserFromAuth(auth);
        if (!authService.isAdmin(me)) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        return userRepository.findById(id).map(u -> { u.setPassword(null); return ResponseEntity.ok(new ApiResponse<>(true, null, u)); }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<User>> updateRole(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        User me = authService.getUserFromAuth(auth);
        if (!authService.isAdmin(me)) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        String role = body.get("role");
        return userRepository.findById(id).map(u -> {
            u.setRole(role);
            userRepository.save(u);
            u.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", u));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<User>> toggleActive(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        User me = authService.getUserFromAuth(auth);
        if (!authService.isAdmin(me)) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        return userRepository.findById(id).map(u -> {
            u.setActive(!u.getActive());
            userRepository.save(u);
            u.setPassword(null);
            return ResponseEntity.ok(new ApiResponse<>(true, "Toggled", u));
        }).orElse(ResponseEntity.notFound().build());
    }
}
