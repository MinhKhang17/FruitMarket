package com.example.fruitmarket.service;

import com.example.fruitmarket.Dto.OrderRequest;
import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.ProductVariant;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public interface OrderService {
    Order createOrder(HttpSession session, ProductVariant variant, Integer quantity, Long addressId, String paymentMethod);

    Long createOrderFromCart(OrderRequest orderReq, HttpSession session);
}
