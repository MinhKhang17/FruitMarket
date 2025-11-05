package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Cart;
import org.springframework.stereotype.Service;

@Service
public interface CartService {
    Cart getCart();

    void addToCart(Long productId, Long variantId, double qty);

    void updateQuantity(Long productId, Long variantId, double qty);

    void remove(Long productId, Long variantId);

    void clear();
}
