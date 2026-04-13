package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.ProductVariant;
import com.zestwear.backend.repositories.ProductVariantRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class ProductVariantController {

    private final ProductVariantRepository variantRepository;
    private final AuthService authService;

    public ProductVariantController(ProductVariantRepository variantRepository, AuthService authService) {
        this.variantRepository = variantRepository;
        this.authService = authService;
    }

    @GetMapping("/api/products/{productId}/variants")
    public ResponseEntity<ApiResponse<List<ProductVariant>>> listByProduct(@PathVariable Long productId) {
        List<ProductVariant> list = variantRepository.findByProductId(productId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, list));
    }

    @PostMapping("/api/products/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariant>> create(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long productId, @RequestBody ProductVariant v) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        v.setProductId(productId);
        ProductVariant saved = variantRepository.save(v);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created", saved));
    }

    @PutMapping("/api/product-variants/{id}")
    public ResponseEntity<ApiResponse<ProductVariant>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody ProductVariant updated) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        return variantRepository.findById(id).map(v -> {
            v.setName(updated.getName());
            v.setSku(updated.getSku());
            v.setPrice(updated.getPrice());
            v.setStock(updated.getStock());
            variantRepository.save(v);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", v));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/product-variants/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).build();
        return variantRepository.findById(id).map(v -> {
            variantRepository.delete(v);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
