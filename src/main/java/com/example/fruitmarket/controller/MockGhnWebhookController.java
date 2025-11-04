package com.example.fruitmarket.controller;

import com.example.fruitmarket.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dev/ghn")
public class MockGhnWebhookController {

    private final OrderService orderService;

    @GetMapping("/orders/{id}/{action}")
    public String mockAndShow(@PathVariable Long id,
                              @PathVariable String action,
                              Model model) {

        // map action -> status GHN
        String status = switch (action.toLowerCase()) {
            case "delivered" -> "delivered";
            case "cancel"    -> "cancel";
            case "picking"   -> "picking";
            default          -> "delivered"; // mặc định cho gọn
        };

        // cập nhật đơn nội bộ
        orderService.updateFromGhnCallback("ORD-" + id, null, status, null);

        model.addAttribute("orderId", id);
        model.addAttribute("status", status);
        // đường dẫn trang chi tiết đơn của bạn (sửa cho khớp app)
        model.addAttribute("orderDetailUrl", "/myOrders/" + id);

        return "mock-result";
    }

}
