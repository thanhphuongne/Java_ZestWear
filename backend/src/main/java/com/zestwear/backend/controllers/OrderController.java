package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Order;
import com.zestwear.backend.models.CartItem;
import com.zestwear.backend.models.OrderItem;
import com.zestwear.backend.models.Product;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.OrderRepository;
import com.zestwear.backend.models.Coupon;
import com.zestwear.backend.repositories.CouponRepository;
import com.zestwear.backend.repositories.UserRepository;
import com.zestwear.backend.repositories.CartItemRepository;
import com.zestwear.backend.repositories.ProductRepository;
import com.zestwear.backend.repositories.OrderItemRepository;
import com.zestwear.backend.repositories.OrderStatusHistoryRepository;
import com.zestwear.backend.models.OrderStatusHistory;
import com.zestwear.backend.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final AuthService authService;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;

    public OrderController(OrderRepository orderRepository, CouponRepository couponRepository, AuthService authService,
                           CartItemRepository cartItemRepository, ProductRepository productRepository, OrderItemRepository orderItemRepository, OrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.couponRepository = couponRepository;
        this.authService = authService;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Order>>> myOrders(@RequestHeader(value = "Authorization", required = false) String auth) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<List<Order>>(false, "Unauthorized", null));
        return ResponseEntity.ok(new ApiResponse<>(true, null, orderRepository.findByUserId(user.getId())));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ApiResponse<Order>> myOrderDetail(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<Order>(false, "Unauthorized", null));
        return orderRepository.findById(id).map(o -> {
            if (!o.getUserId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse<Order>(false, "Forbidden", null));
            o.setItems(orderItemRepository.findByOrderId(o.getId()));
            return ResponseEntity.ok(new ApiResponse<>(true, null, o));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Order>(false, "Not Found", null)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestHeader(value = "Authorization", required = false) String auth, @RequestBody Map<String, Object> body) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));

        // determine payment method (frontend may send an integer code)
        String paymentMethod = "CreditCard";
        try {
            Object pmObj = body.getOrDefault("paymentMethod", null);
            if (pmObj != null) {
                int pm = Integer.parseInt(String.valueOf(pmObj));
                switch (pm) {
                    case 0 -> paymentMethod = "COD";
                    case 1 -> paymentMethod = "BankTransfer";
                    case 2 -> paymentMethod = "Momo";
                    case 3 -> paymentMethod = "VNPay";
                    default -> paymentMethod = "CreditCard";
                }
            }
        } catch (Exception ignored) { }

        // Build order from cart items
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems == null || cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<Map<String, Object>>(false, "Cart is empty", null));
        }

        double subTotal = cartItems.stream().mapToDouble(i -> i.getUnitPrice() * i.getQuantity()).sum();
        double shipping = 30000.0; // fixed shipping like .NET example
        double totalAmount = subTotal + shipping;

        // apply coupon if provided
        String couponCode = null;
        try { couponCode = body.getOrDefault("couponCode", null) == null ? null : String.valueOf(body.get("couponCode")); } catch (Exception ignored) { }
        Coupon applied = null;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon c = couponRepository.findByCode(couponCode);
            if (c != null && Boolean.TRUE.equals(c.getActive())) {
                if (c.getExpiresAt() == null || c.getExpiresAt().isAfter(java.time.LocalDateTime.now())) {
                    if (c.getMinOrderAmount() == null || subTotal >= c.getMinOrderAmount()) {
                        if (c.getUsageLimit() == null || c.getUsedCount() == null || c.getUsedCount() < c.getUsageLimit()) {
                            double discount = 0.0;
                            if ("PERCENT".equalsIgnoreCase(c.getType())) discount = subTotal * c.getAmount() / 100.0;
                            else discount = c.getAmount();
                            if (discount > totalAmount) discount = totalAmount;
                            totalAmount = Math.max(0.0, totalAmount - discount);
                            applied = c;
                        }
                    }
                }
            }
        }

        Order order = new Order(user.getId(), "PENDING", totalAmount, UUID.randomUUID().toString());
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);

        // record initial status history
        OrderStatusHistory h0 = new OrderStatusHistory(order.getId(), order.getStatus(), "Created", user.getId());
        historyRepository.save(h0);

        // persist order items and adjust stock
        for (CartItem c : cartItems) {
            Long prodId = c.getProductId();
            Product prod = productRepository.findById(prodId).orElse(null);
            double unitPrice = c.getUnitPrice();
            int qty = c.getQuantity() == null ? 1 : c.getQuantity();

            OrderItem oi = new OrderItem(order.getId(), prodId, c.getProductName(), qty, unitPrice, unitPrice * qty);
            orderItemRepository.save(oi);

            if (prod != null && prod.getStock() != null) {
                int newStock = Math.max(0, prod.getStock() - qty);
                prod.setStock(newStock);
                productRepository.save(prod);
            }
        }

        // clear cart
        cartItemRepository.deleteAll(cartItems);

        // mark coupon used if applied
        if (applied != null) {
            applied.setUsedCount((applied.getUsedCount() == null ? 0 : applied.getUsedCount()) + 1);
            couponRepository.save(applied);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        data.put("orderCode", order.getOrderCode());
        return ResponseEntity.ok(new ApiResponse<Map<String, Object>>(true, "Created", data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> all(@RequestHeader(value = "Authorization", required = false) String auth) {
        // admin-only
        User me = authService.getUserFromAuth(auth);
        if (!authService.isAdmin(me)) return ResponseEntity.status(403).body(new ApiResponse<List<Order>>(false, "Forbidden", null));
        return ResponseEntity.ok(new ApiResponse<>(true, null, orderRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getById(@PathVariable Long id) {
        return orderRepository.findById(id).map(o -> {
            o.setItems(orderItemRepository.findByOrderId(o.getId()));
            return ResponseEntity.ok(new ApiResponse<>(true, null, o));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Order>(false, "Not Found", null)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateStatus(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return orderRepository.findById(id).map(o -> {
            String newStatus = body.getOrDefault("status", o.getStatus());
            String note = body.getOrDefault("note", null);
            o.setStatus(newStatus);
            orderRepository.save(o);
            // save history
            Long actorId = null;
            try { actorId = authService.getUserFromAuth(auth).getId(); } catch (Exception ignored) {}
            OrderStatusHistory h = new OrderStatusHistory(o.getId(), newStatus, note, actorId);
            historyRepository.save(h);
            return ResponseEntity.ok(new ApiResponse<>(true, "Updated", o));
        }).orElse(ResponseEntity.status(404).body(new ApiResponse<Order>(false, "Not Found", null)));
    }

    @PostMapping("/validate-coupon")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCoupon(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.get("code"));
        Double orderAmount = Double.valueOf(String.valueOf(body.getOrDefault("orderAmount", 0)));
        Coupon coupon = couponRepository.findByCode(code);
        if (coupon == null || !coupon.getActive()) {
            return ResponseEntity.ok(new ApiResponse<Map<String, Object>>(false, "Invalid coupon", null));
        }
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.ok(new ApiResponse<Map<String, Object>>(false, "Coupon expired", null));
        }
        if (orderAmount < coupon.getMinOrderAmount()) {
            return ResponseEntity.ok(new ApiResponse<Map<String, Object>>(false, "Minimum order amount not met", null));
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return ResponseEntity.ok(new ApiResponse<Map<String, Object>>(false, "Coupon usage limit reached", null));
        }
        double discount = 0.0;
        if ("PERCENT".equalsIgnoreCase(coupon.getType())) {
            discount = orderAmount * coupon.getAmount() / 100.0;
        } else {
            discount = coupon.getAmount();
        }
        if (discount > orderAmount) discount = orderAmount;
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("discountAmount", discount);
        data.put("newTotal", Math.max(0.0, orderAmount - discount));
        data.put("coupon", coupon);
        return ResponseEntity.ok(new ApiResponse<Map<String, Object>>(true, "OK", data));
    }
}
