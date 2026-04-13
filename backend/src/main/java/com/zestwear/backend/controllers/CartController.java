package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.CartItem;
import com.zestwear.backend.models.Product;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.CartItemRepository;
import com.zestwear.backend.repositories.ProductRepository;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;

    public CartController(CartItemRepository cartItemRepository, ProductRepository productRepository, AuthService authService) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        List<CartItem> items = cartItemRepository.findByUserId(user.getId());
        int totalItems = items.stream().mapToInt(i -> i.getQuantity()).sum();
        double subTotal = items.stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("totalItems", totalItems);
        data.put("subTotal", subTotal);
        return ResponseEntity.ok(new ApiResponse<>(true, null, data));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        Long count = cartItemRepository.countByUserId(user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, null, count));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addItem(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Map<String, Object> body) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        Integer quantity = Integer.valueOf(String.valueOf(body.getOrDefault("quantity", 1)));
        Optional<Product> pOpt = productRepository.findById(productId);
        if (pOpt.isEmpty()) return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Product not found", null));
        Product p = pOpt.get();
        CartItem item = new CartItem(user.getId(), p.getId(), p.getName(), quantity, p.getPrice().doubleValue());
        cartItemRepository.save(item);
        // return full cart
        return getCart(auth);
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateItem(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long cartItemId, @RequestBody Map<String, Object> body) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        Integer quantity = Integer.valueOf(String.valueOf(body.getOrDefault("quantity", 1)));
        Optional<CartItem> it = cartItemRepository.findById(cartItemId);
        if (it.isPresent() && it.get().getUserId().equals(user.getId())) {
            CartItem item = it.get();
            item.setQuantity(quantity);
            cartItemRepository.save(item);
            return getCart(auth);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeItem(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long cartItemId) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        Optional<CartItem> it = cartItemRepository.findById(cartItemId);
        if (it.isPresent() && it.get().getUserId().equals(user.getId())) {
            cartItemRepository.delete(it.get());
            return getCart(auth);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Object>> clear(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        List<CartItem> items = cartItemRepository.findByUserId(user.getId());
        cartItemRepository.deleteAll(items);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cleared", null));
    }
}
