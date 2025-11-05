package com.example.fruitmarket.controller;

import com.example.fruitmarket.enums.GhnStatus;
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

        GhnStatus status = switch (action.toLowerCase()) {
            case "delivered" -> GhnStatus.DELIVERED;
            case "cancel"    -> GhnStatus.CANCEL;
            case "picking"   -> GhnStatus.PICKING;
            default -> null;
        };

        if (status == null) {
            return "unknown action: " + action;
        }

        boolean updated;
        try {
            updated = orderService.updateFromGhnCallback(id, null, status, null);
        } catch (Exception ex) {
            return "update status order failed: " + ex.getMessage();
        }

        if (!updated) {
            return "update status order not success";
        }

        model.addAttribute("orderId", id);
        model.addAttribute("status", status);
        model.addAttribute("orderDetailUrl", "/myOrders/" + id);
        return "update status order success";
    }
}

