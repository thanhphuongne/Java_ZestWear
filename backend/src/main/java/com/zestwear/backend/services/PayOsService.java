package com.zestwear.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestwear.backend.models.Order;
import com.zestwear.backend.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PayOsService {
    private static final Logger log = LoggerFactory.getLogger(PayOsService.class);
    private final OrderRepository orderRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${app.payos.enabled:false}")
    private boolean enabled;

    @Value("${app.payos.client-id:}")
    private String clientId;

    @Value("${app.payos.api-key:}")
    private String apiKey;

    @Value("${app.payos.checksum-key:}")
    private String checksumKey;

    @Value("${app.payos.base-url:}")
    private String baseUrl;

    public PayOsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public boolean isEnabled() { return enabled && baseUrl != null && !baseUrl.isBlank(); }

    public Map<String, Object> createPaymentLink(Order order, String returnUrl, String cancelUrl) throws Exception {
        // Build numeric orderCode similar to .NET logic
        String digits = order.getOrderCode().chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (digits.isEmpty()) digits = String.valueOf(System.currentTimeMillis() / 1000L);
        String composite = digits + String.valueOf(order.getId());
        if (composite.length() > 18) composite = composite.substring(composite.length() - 18);
        long orderCodeNumeric;
        try { orderCodeNumeric = Long.parseLong(composite); } catch (Exception ex) { orderCodeNumeric = System.currentTimeMillis() / 1000L; }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", orderCodeNumeric);
        payload.put("amount", Math.max(0L, Math.round(order.getTotalAmount() == null ? 0.0 : order.getTotalAmount())));
        payload.put("description", "Thanh toan " + order.getOrderCode());
        payload.put("returnUrl", returnUrl);
        payload.put("cancelUrl", cancelUrl);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        String name = "Order " + (order.getOrderCode() == null ? "" : order.getOrderCode());
        if (name.length() > 25) name = name.substring(0, 25);
        item.put("name", name);
        item.put("quantity", 1);
        item.put("price", Math.max(0L, Math.round(order.getTotalAmount() == null ? 0.0 : order.getTotalAmount())));
        items.add(item);
        payload.put("items", items);

        String checkoutUrl = null;
        if (isEnabled()) {
            try {
                String url = baseUrl.endsWith("/") ? baseUrl + "v2/payment-requests" : baseUrl + "/v2/payment-requests";
                String body = mapper.writeValueAsString(payload);

                HttpRequest.Builder reqb = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

                // Add common header variations for client id / api key
                if (clientId != null && !clientId.isBlank()) {
                    reqb.header("X-Client-Id", clientId);
                    reqb.header("Client-Id", clientId);
                }
                if (apiKey != null && !apiKey.isBlank()) {
                    reqb.header("X-Api-Key", apiKey);
                    reqb.header("Api-Key", apiKey);
                }

                HttpRequest req = reqb.build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                String respBody = resp.body();
                checkoutUrl = tryFindCheckoutInJsonString(respBody);
                if (checkoutUrl == null) {
                    // try extract http link from plain text
                    checkoutUrl = tryExtractUrlFromText(respBody);
                }
            } catch (Exception ex) {
                log.debug("PayOS create request failed: {}", ex.getMessage());
            }
        }

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            log.warn("PayOS checkout URL not available; falling back to mock link for order {}", order.getOrderCode());
            checkoutUrl = (returnUrl == null ? "" : returnUrl.replaceAll("/+$", "")) + "?orderCode=" + order.getOrderCode() + "&mock=1";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("checkoutUrl", checkoutUrl);
        result.put("orderCode", order.getOrderCode());
        return result;
    }

    public String lookupExistingPayOsCheckoutUrl(String orderCodeOrKey) {
        if (orderCodeOrKey == null) return null;
        String digits = orderCodeOrKey.chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (digits.isEmpty()) return null;

        if (!isEnabled()) return null;
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "v2/payment-requests?orderCode=" + digits : baseUrl + "/v2/payment-requests?orderCode=" + digits;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            String found = tryFindCheckoutInJsonString(resp.body());
            if (found != null) return found;
        } catch (Exception ex) {
            log.debug("PayOS lookup failed: {}", ex.getMessage());
        }
        return null;
    }

    public void handleWebhook(String rawBody, String signatureHeader) throws Exception {
        if (checksumKey != null && !checksumKey.isBlank()) {
            if (signatureHeader == null || signatureHeader.isBlank()) throw new SecurityException("Missing webhook signature");
            String sig = signatureHeader.trim();
            int eq = sig.indexOf('=');
            if (eq >= 0) sig = sig.substring(eq + 1);

            byte[] keyBytes;
            try {
                keyBytes = HexFormat.of().parseHex(checksumKey);
            } catch (Exception ex) {
                try { keyBytes = Base64.getDecoder().decode(checksumKey); }
                catch (Exception ex2) { keyBytes = checksumKey.getBytes(StandardCharsets.UTF_8); }
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] bodyBytes = rawBody.getBytes(StandardCharsets.UTF_8);
            byte[] computed = mac.doFinal(bodyBytes);
            String computedBase64 = Base64.getEncoder().encodeToString(computed);
            String computedHex = HexFormat.of().formatHex(computed).toLowerCase(Locale.ROOT);

            String canonical = null;
            try {
                JsonNode n = mapper.readTree(rawBody);
                canonical = mapper.writeValueAsString(n);
            } catch (Exception ignored) { }

            String canonicalBase64 = null;
            String canonicalHex = null;
            if (canonical != null) {
                byte[] cb = canonical.getBytes(StandardCharsets.UTF_8);
                byte[] comp2 = mac.doFinal(cb);
                canonicalBase64 = Base64.getEncoder().encodeToString(comp2);
                canonicalHex = HexFormat.of().formatHex(comp2).toLowerCase(Locale.ROOT);
            }

            String sigNorm = sig.trim();
            if (!(sigNorm.equals(computedBase64) || sigNorm.equalsIgnoreCase(computedHex) || (canonicalBase64 != null && sigNorm.equals(canonicalBase64)) || (canonicalHex != null && sigNorm.equalsIgnoreCase(canonicalHex)))) {
                throw new SecurityException("Invalid webhook signature");
            }
        }

        JsonNode doc = mapper.readTree(rawBody);
        long orderCodeNumeric = 0;
        if (doc.has("OrderCode")) orderCodeNumeric = doc.get("OrderCode").asLong(0L);
        else if (doc.has("orderCode")) orderCodeNumeric = doc.get("orderCode").asLong(0L);
        if (orderCodeNumeric == 0) return;

        String key = String.valueOf(orderCodeNumeric);
        List<Order> orders = orderRepository.findByOrderCodeContaining(key);
        if (orders == null || orders.isEmpty()) return;
        Order order = orders.get(0);

        String code = "";
        if (doc.has("Code")) code = doc.get("Code").asText("");
        else if (doc.has("code")) code = doc.get("code").asText("");

        String reference = "";
        if (doc.has("Reference")) reference = doc.get("Reference").asText("");
        else if (doc.has("reference")) reference = doc.get("reference").asText("");

        if ("00".equals(code)) {
            order.setPaymentStatus("Paid");
            order.setPaidAt(LocalDateTime.now());
            if ("PENDING".equalsIgnoreCase(order.getStatus())) order.setStatus("CONFIRMED");
        } else {
            order.setPaymentStatus("Failed");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private String tryFindCheckoutInJsonString(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            JsonNode root = mapper.readTree(s);
            if (root.has("data")) {
                JsonNode data = root.get("data");
                if (data.has("checkoutUrl")) return data.get("checkoutUrl").asText(null);
                if (data.has("checkout_url")) return data.get("checkout_url").asText(null);
                Iterator<Map.Entry<String, JsonNode>> it = data.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if (e.getKey().toLowerCase().contains("checkout") && e.getValue().isTextual()) return e.getValue().asText(null);
                }
            }
            if (root.has("checkoutUrl")) return root.get("checkoutUrl").asText(null);
            if (root.has("checkout_url")) return root.get("checkout_url").asText(null);
        } catch (Exception ignored) { }
        return null;
    }

    private String tryExtractUrlFromText(String text) {
        if (text == null || text.isBlank()) return null;
        int idx = text.indexOf("http");
        if (idx >= 0) {
            int end = text.indexOf(' ', idx);
            if (end < 0) end = text.length();
            String url = text.substring(idx, end).replaceAll("[\"']$", "");
            if (url.startsWith("http")) return url;
        }
        return null;
    }
}
