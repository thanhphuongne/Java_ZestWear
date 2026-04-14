package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Product;
import com.zestwear.backend.repositories.ProductRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final AuthService authService;

    public ProductController(ProductRepository productRepository, AuthService authService) {
        this.productRepository = productRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "0") int perPage
    ) {
        List<Product> all = productRepository.findAll();
        List<Product> filtered = all.stream().filter(p -> {
            boolean ok = true;
            if (search != null && !search.isBlank()) {
                String s = search.toLowerCase();
                ok = (p.getName() != null && p.getName().toLowerCase().contains(s)) || (p.getDescription() != null && p.getDescription().toLowerCase().contains(s));
            }
            if (ok && categoryId != null) {
                ok = p.getCategoryId() != null && p.getCategoryId().equals(categoryId);
            }
            return ok;
        }).toList();

        if (perPage > 0) {
            int start = Math.max(0, (page - 1) * perPage);
            int end = Math.min(start + perPage, filtered.size());
            List<Product> pageItems = start < end ? filtered.subList(start, end) : List.of();
            return ResponseEntity.ok(new ApiResponse<>(true, null, pageItems));
        }

        return ResponseEntity.ok(new ApiResponse<>(true, null, filtered));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<Product>>> featured(@RequestParam(defaultValue = "8") int count) {
        List<Product> items = productRepository.findByFeaturedTrue(PageRequest.of(0, count));
        return ResponseEntity.ok(new ApiResponse<>(true, null, items));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Long id) {
        return productRepository.findById(id)
            .map(p -> ResponseEntity.ok(new ApiResponse<>(true, null, p)))
            .orElse(ResponseEntity.status(404).body(new ApiResponse<Product>(false, "Not Found", null)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<Product>> getBySlug(@PathVariable String slug) {
        Product p = productRepository.findBySlug(slug);
        if (p == null) return ResponseEntity.status(404).body(new ApiResponse<Product>(false, "Not Found", null));
        return ResponseEntity.ok(new ApiResponse<>(true, null, p));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Product product) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<Product>(false, "Forbidden", null));
        if (product.getSlug() == null || product.getSlug().isEmpty()) {
            product.setSlug(product.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }
        Product saved = productRepository.save(product);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Product updated) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<Product>(false, "Forbidden", null));
        return productRepository.findById(id).map(p -> {
            p.setName(updated.getName());
            p.setDescription(updated.getDescription());
            p.setPrice(updated.getPrice());
            p.setImageUrl(updated.getImageUrl());
            p.setImages(updated.getImages());
            p.setStock(updated.getStock());
            p.setSlug(updated.getSlug());
            p.setFeatured(updated.getFeatured());
            p.setCategoryId(updated.getCategoryId());
            productRepository.save(p);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", p));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Product>(false, "Not Found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).build();
        return productRepository.findById(id).map(p -> {
            productRepository.delete(p);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<List<Product>>> related(@PathVariable Long id, @RequestParam(defaultValue = "4") int count) {
        return productRepository.findById(id).map(p -> {
            List<Product> list = productRepository.findAll();
            // naive: filter by same category
            List<Product> related = list.stream()
                    .filter(x -> x.getId() != null && !x.getId().equals(id) && x.getCategoryId() != null && x.getCategoryId().equals(p.getCategoryId()))
                    .limit(count)
                    .toList();
            return ResponseEntity.ok(new ApiResponse<>(true, null, related));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<List<Product>>(false, "Not Found", null)));
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestHeader(value = "Authorization", required = false) String auth, @RequestPart("file") MultipartFile file) {
        if (!authService.isStaffOrAdmin(authService.getUserFromAuth(auth))) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        try {
            String filename = StringUtils.cleanPath(file.getOriginalFilename());
            String ext = "";
            int i = filename.lastIndexOf('.');
            if (i >= 0) ext = filename.substring(i);
            String newName = UUID.randomUUID().toString() + ext;
            Path uploadDir = Paths.get("src/main/resources/static/images");
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(newName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = "/images/" + newName;
            return ResponseEntity.ok(new ApiResponse<>(true, "Uploaded", url));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
