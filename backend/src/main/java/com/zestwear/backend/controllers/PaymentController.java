package com.zestwear.backend.controllers;

import com.zestwear.backend.dto.ApiResponse;
import com.zestwear.backend.models.Order;
import com.zestwear.backend.models.User;
import com.zestwear.backend.repositories.OrderRepository;
import com.zestwear.backend.services.AuthService;
import com.zestwear.backend.services.PayOsService;
import com.zestwear.backend.services.StripeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final StripeService stripeService;
    private final OrderRepository orderRepository;
    private final PayOsService payOsService;
    private final AuthService authService;

    @Value("${app.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public PaymentController(StripeService stripeService, OrderRepository orderRepository, PayOsService payOsService, AuthService authService) {
        this.stripeService = stripeService;
        this.orderRepository = orderRepository;
        this.payOsService = payOsService;
        this.authService = authService;
    }

    @PostMapping("/create-link")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createLink(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        // auth
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));

        Object oid = body.get("orderId");
        if (oid == null) return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Missing orderId", null));
        Long orderId;
        try { orderId = Long.valueOf(String.valueOf(oid)); } catch (Exception ex) { return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Invalid orderId", null)); }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        if (!order.getUserId().equals(user.getId())) return ResponseEntity.status(403).body(new ApiResponse<>(false, "Forbidden", null));

        // Determine frontend base (allowed origins)
        String[] allowed = allowedOrigins.split(",");
        for (int i = 0; i < allowed.length; i++) allowed[i] = allowed[i].trim().replaceAll("/+$", "");

        String frontendBase = null;
        Object provided = body.get("returnUrl");
        if (provided instanceof String && !((String) provided).isBlank()) {
            try {
                URI prov = URI.create((String) provided);
                String origin = prov.getScheme() + "://" + prov.getHost() + (prov.getPort() == -1 ? "" : ":" + prov.getPort());
                for (String a : allowed) if (a.equalsIgnoreCase(origin.replaceAll("/+$", ""))) { frontendBase = origin; break; }
            } catch (Exception ignored) { }
        }

        if (frontendBase == null) {
            String originHeader = request.getHeader("Origin");
            if (originHeader != null && !originHeader.isBlank()) {
                try {
                    URI originUri = URI.create(originHeader);
                    String origin = originUri.getScheme() + "://" + originUri.getHost() + (originUri.getPort() == -1 ? "" : ":" + originUri.getPort());
                    for (String a : allowed) if (a.equalsIgnoreCase(origin.replaceAll("/+$", ""))) { frontendBase = origin; break; }
                } catch (Exception ignored) { }
            }
        }

        if (frontendBase == null && allowed.length > 0) frontendBase = allowed[0];

        String returnUrl = frontendBase + "/payment/success";
        String cancelUrl = frontendBase + "/payment/cancel";

        try {
            if (payOsService.isEnabled()) {
                Map<String, Object> res = payOsService.createPaymentLink(order, returnUrl, cancelUrl);
                return ResponseEntity.ok(new ApiResponse<>(true, "OK", res));
            }

            // Fallback to Stripe if PayOS not enabled
            String url = stripeService.createCheckoutSession(order, returnUrl + "?session_id={CHECKOUT_SESSION_ID}", cancelUrl);
            Map<String, Object> data = new HashMap<>();
            data.put("checkoutUrl", url);
            return ResponseEntity.ok(new ApiResponse<>(true, "OK", data));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(new ApiResponse<>(false, ex.getMessage(), null));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader(value = "Stripe-Signature", required = false) String stripeSig,
                                          HttpServletRequest request) {
        try {
            // Try Stripe first if signature provided
            if (stripeSig != null && !stripeSig.isBlank()) {
                try {
                    stripeService.handleWebhook(payload, stripeSig);
                    return ResponseEntity.ok("Received");
                } catch (Exception ex) {
                    // fallthrough to try PayOS
                }
            }

            // Try PayOS signature headers
            String[] signatureHeaders = new String[]{"X-PayOS-Signature", "X-PAYOS-SIGNATURE", "X-Payos-Signature", "X-PayOS-Checksum", "X-Checksum", "Checksum", "Signature", "X-Signature"};
            String payosSig = null;
            for (String h : signatureHeaders) {
                String v = request.getHeader(h);
                if (v != null && !v.isBlank()) { payosSig = v; break; }
            }

            if (payosSig != null && !payosSig.isBlank()) {
                payOsService.handleWebhook(payload, payosSig);
                return ResponseEntity.ok("Received");
            }

            return ResponseEntity.status(400).body("Webhook error: no known signature header found");
        } catch (Exception ex) {
            return ResponseEntity.status(400).body("Webhook error: " + ex.getMessage());
        }
    }

    @GetMapping("/debug/payos/{orderCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugLookupPayOs(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable String orderCode) {
        User user = authService.getUserFromAuth(auth);
        if (user == null) return ResponseEntity.status(401).body(new ApiResponse<>(false, "Unauthorized", null));
        try {
            String url = payOsService.lookupExistingPayOsCheckoutUrl(orderCode);
            if (url == null) return ResponseEntity.status(404).body(new ApiResponse<>(false, "No checkout URL found", null));
            Map<String, Object> data = new HashMap<>();
            data.put("checkoutUrl", url);
            return ResponseEntity.ok(new ApiResponse<>(true, "OK", data));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(new ApiResponse<>(false, ex.getMessage(), null));
        }
    }
}
