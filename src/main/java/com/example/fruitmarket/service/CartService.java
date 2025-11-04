package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Cart;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface CartService {
    Cart getCart();

    void addToCart(Long productId, Long variantId, int qty);

    void updateQuantity(Long productId, Long variantId, int qty);

    void remove(Long productId, Long variantId);

    void clear();

    int getTotalQuantity(HttpSession s);
    BigDecimal getSubtotal(HttpSession s);
}
