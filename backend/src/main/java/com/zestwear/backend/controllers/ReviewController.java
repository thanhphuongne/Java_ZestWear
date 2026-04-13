package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Review;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.ReviewRepository;
import com.zestwear.backend.repositories.UserRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository, AuthService authService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<Review>>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(new ApiResponse<>(true, null, reviewRepository.findByProductId(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Review>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Review review) {
        User me = authService.getUserFromAuth(auth);
        if (me == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        review.setUserId(me.getId());
        Review saved = reviewRepository.save(review);
        return ResponseEntity.ok(new ApiResponse<>(true, "Created", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        User me = authService.getUserFromAuth(auth);
        return reviewRepository.findById(id).map(r -> {
            if (me == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
            if (r.getUserId() != null && r.getUserId().equals(me.getId()) || authService.isAdmin(me)) {
                reviewRepository.deleteById(id);
                return ResponseEntity.ok(new ApiResponse<>(true, "Deleted", null));
            }
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));
        }).orElse(ResponseEntity.notFound().build());
    }
}
