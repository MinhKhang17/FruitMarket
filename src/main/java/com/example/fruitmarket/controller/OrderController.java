package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String myOrders(HttpSession session, Model model) {
        try {
            List<Order> orders = orderService.getOrdersOfUser(session);
            model.addAttribute("orders", orders);
        } catch (IllegalStateException e) {
            return "redirect:/auth/login";
        }
        return "client/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Long id, HttpSession session, Model model) {
        try {
            Order order = orderService.getOrderDetailForUser(id, session);
            model.addAttribute("order", order);
            return "client/order-detail";
        } catch (IllegalStateException ex) {
            return "redirect:/auth/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "client/order-detail";
        }
    }
}
