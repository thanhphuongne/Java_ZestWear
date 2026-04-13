package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Banner;
import com.zestwear.backend.repositories.BannerRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/banners")
public class BannerController {
    private final BannerRepository bannerRepository;
    private final AuthService authService;

    public BannerController(BannerRepository bannerRepository, AuthService authService) {
        this.bannerRepository = bannerRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Banner>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(true, null, bannerRepository.findByActiveTrueOrderByOrderIndexAsc()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Banner>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Banner banner) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        Banner saved = bannerRepository.save(banner);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Banner>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Banner updated) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        return bannerRepository.findById(id).map(b -> {
            b.setImageUrl(updated.getImageUrl());
            b.setActive(updated.getActive());
            b.setOrderIndex(updated.getOrderIndex());
            bannerRepository.save(b);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", b));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        if (!authService.isAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).build();
        return bannerRepository.findById(id).map(b -> {
            bannerRepository.delete(b);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
