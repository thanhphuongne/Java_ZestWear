package com.zestwear.backend.repositories;

import com.zestwear.backend.models.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserId(Long userId);
    Long countByUserId(Long userId);
}
