package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.enums.GhnStatus;
import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.ProductVariant;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public interface OrderService {
    Order createOrder(HttpSession session, ProductVariant variant, Integer quantity, Long addressId, String paymentMethod, BigDecimal shippingFee, Integer serviceId);

    Long createOrderFromCart(OrderRequest orderReq, HttpSession session);
    Order getOrderById(Long Id);
    void updateOrder(Order order);
    List<Order> getOrdersOfUser(HttpSession session);
    Order getOrderDetailForUser(Long orderId, HttpSession session);
    List<Order> getAllOrders();
    void attachShippingCode(Long orderId, String ghnOrderCode);
    boolean updateFromGhnCallback(long clientOrderCode, String ghnOrderCode, GhnStatus ghnStatus, Integer codAmount);

    void updateShippingFee(Long orderId, BigDecimal bigDecimal);

    void addShippingToTotal(Long orderId, BigDecimal fee);
}
