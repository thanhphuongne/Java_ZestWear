package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Category;
import com.zestwear.backend.repositories.CategoryRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public CategoryController(CategoryRepository categoryRepository, AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(true, null, categoryRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> get(@PathVariable Long id) {
        return categoryRepository.findById(id)
            .map(cat -> ResponseEntity.ok(new ApiResponse<>(true, null, cat)))
            .orElse(ResponseEntity.status(404).body(new ApiResponse<Category>(false, "Not Found", null)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Category category) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<Category>(false, "Forbidden", null));
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Category updated) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<Category>(false, "Forbidden", null));
        return categoryRepository.findById(id).map(c -> {
            c.setName(updated.getName());
            c.setSlug(updated.getSlug());
            categoryRepository.save(c);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", c));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Category>(false, "Not Found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).build();
        return categoryRepository.findById(id).map(c -> {
            categoryRepository.delete(c);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
