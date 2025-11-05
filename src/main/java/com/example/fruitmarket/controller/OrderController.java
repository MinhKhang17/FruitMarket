package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.OrderDetailRes;
import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.service.GhnClientService;
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
    private final GhnClientService ghnClientService;

    @GetMapping("/myOrders")
    public String myOrders(HttpSession session, Model model) {
        try {
            List<Order> orders = orderService.getOrdersOfUser(session);
            model.addAttribute("orders", orders);
        } catch (IllegalStateException e) {
            return "redirect:/auth/login";
        }
        return "client/ordersClient"; // <-- phải khớp với ordersClient.html
    }

    @GetMapping("/myOrders/{id}")
    public String orderDetail(@PathVariable Long id, HttpSession session, Model model) {
        try {
            Order order = orderService.getOrderDetailForUser(id, session);
            model.addAttribute("order", order);

            try {
                // ✅ Lấy mã GHN thực tế từ order đã lưu
                String code = order.getGhnOrderCode();
                if (code != null && !code.isBlank()) {
                    OrderDetailRes detail = ghnClientService.getOrderDetail(code);
                    if (detail != null && detail.getData() != null) {
                        model.addAttribute("ghn", detail.getData());
                    }
                }
            } catch (Exception e) {
                model.addAttribute("ghnError", "Không thể tải trạng thái GHN lúc này: " + e.getMessage());
            }

            return "client/order-detail"; // khớp với order-detail.html
        } catch (IllegalStateException ex) {
            return "redirect:/auth/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "client/order-detail";
        }
    }
}
