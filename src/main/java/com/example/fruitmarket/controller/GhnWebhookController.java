package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.GhnOrderStatusPayload;
import com.example.fruitmarket.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GHN sẽ POST vào URL này khi trạng thái đơn thay đổi.
 * Bạn cần cấu hình URL này (public HTTPS) trong trang quản trị GHN.
 */
@RestController
@RequestMapping("/webhooks/ghn")
@RequiredArgsConstructor
public class GhnWebhookController {

     private final OrderService orderService; // cắm service của bạn để cập nhật DB

    @PostMapping("/order-status")
    public ResponseEntity<Void> onOrderStatus(@RequestBody GhnOrderStatusPayload payload) {
        // Ví dụ: map trạng thái GHN -> trạng thái nội bộ
         orderService.updateFromGhnCallback(
                payload.getClientOrderCode(),
                payload.getOrderCode(),
                payload.getStatus(),
                payload.getCODAmount()
         );

        // Trả 200 OK để GHN biết bạn nhận thành công
        return ResponseEntity.ok().build();
    }
}
