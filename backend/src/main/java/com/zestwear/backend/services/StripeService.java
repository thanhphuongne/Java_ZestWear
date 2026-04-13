package com.zestwear.backend.services;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.zestwear.backend.models.Order;
import com.zestwear.backend.repositories.OrderRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StripeService {

    @Value("${app.stripe.secret:}")
    private String stripeSecret;

    @Value("${app.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    private final OrderRepository orderRepository;

    public StripeService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void init() {
        if (stripeSecret != null && !stripeSecret.isBlank()) {
            Stripe.apiKey = stripeSecret;
        }
    }

    public String createCheckoutSession(Order order, String successUrl, String cancelUrl) throws StripeException {
        long amount = Math.max(0L, Math.round(order.getTotalAmount() * 100));

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amount)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Order " + order.getOrderCode())
                                        .build())
                                .build())
                        .build())
                .putMetadata("orderId", String.valueOf(order.getId()))
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public void handleWebhook(String payload, String sigHeader) throws Exception {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret not configured");
        }
        Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        String type = event.getType();
        if ("checkout.session.completed".equals(type)) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session != null) {
                Map<String, String> metadata = session.getMetadata();
                String orderId = metadata != null ? metadata.get("orderId") : null;
                if (orderId != null) {
                    try {
                        Long id = Long.parseLong(orderId);
                        orderRepository.findById(id).ifPresent(o -> {
                            o.setStatus("PAID");
                            orderRepository.save(o);
                        });
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
    }
}
