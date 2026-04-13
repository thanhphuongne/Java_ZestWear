package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Coupon;
import com.zestwear.backend.repositories.CouponRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponRepository couponRepository;
    private final AuthService authService;

    public CouponController(CouponRepository couponRepository, AuthService authService) {
        this.couponRepository = couponRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Coupon>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(true, null, couponRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Coupon>> get(@PathVariable Long id) {
        return couponRepository.findById(id).map(c -> ResponseEntity.ok(new ApiResponse<>(true, null, c))).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Coupon>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Coupon coupon) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Coupon>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Coupon updated) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        return couponRepository.findById(id).map(c -> {
            c.setCode(updated.getCode());
            c.setType(updated.getType());
            c.setAmount(updated.getAmount());
            c.setActive(updated.getActive());
            c.setMinOrderAmount(updated.getMinOrderAmount());
            c.setExpiresAt(updated.getExpiresAt());
            couponRepository.save(c);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).build();
        return couponRepository.findById(id).map(c -> {
            couponRepository.delete(c);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
