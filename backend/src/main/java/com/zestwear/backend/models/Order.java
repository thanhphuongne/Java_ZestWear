package com.zestwear.backend.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String status;
    private Double totalAmount;
    private String orderCode;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String paymentStatus = "Pending";
    private String paymentMethod = "COD";
    private LocalDateTime paidAt;
    private LocalDateTime updatedAt;

    @Transient
    private List<com.zestwear.backend.models.OrderItem> items;

    public List<com.zestwear.backend.models.OrderItem> getItems() { return items; }
    public void setItems(List<com.zestwear.backend.models.OrderItem> items) { this.items = items; }

    public Order() {}

    public Order(Long userId, String status, Double totalAmount, String orderCode) {
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.orderCode = orderCode;
    }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
