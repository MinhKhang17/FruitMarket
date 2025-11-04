// src/main/java/com/example/fruitmarket/controller/ShippingQuoteController.java
package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.AvailableServicesRes;
import com.example.fruitmarket.dto.FeeRes;
import com.example.fruitmarket.model.Users;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.service.CartService;
import com.example.fruitmarket.service.GhnClientService;
import com.example.fruitmarket.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipping")
public class ShippingController {

    private final GhnClientService ghn;
    private final UserService userService;
    private final CartService cartService;

    @Value("${ghn.from-district-id}")
    private int fromDistrictId;

    @Value("${ghn.default.item-weight:500}") private int defaultItemWeight;
    @Value("${ghn.default.length:20}")       private int defLen;
    @Value("${ghn.default.width:15}")        private int defWid;
    @Value("${ghn.default.height:10}")       private int defHei;

    /** Gọi khi user chọn/đổi addressId: tính phí & LƯU VÀO SESSION */
    @GetMapping("/quote")
    public ResponseEntity<?> quote(@RequestParam Long addressId, HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).build();

        User_detail addr = userService.findUserDetalById(addressId);
        if (addr == null || addr.getDistrict() == null || addr.getWard() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Địa chỉ thiếu district/ward"));
        }
        int toDistrictId = addr.getDistrict().getDistrictId();
        String toWardCode = addr.getWard().getWardCode();

        // 1) Dịch vụ khả dụng
        AvailableServicesRes svRes = ghn.availableServices(fromDistrictId, toDistrictId);
        List<AvailableServicesRes.ServiceItem> services =
                (svRes != null && svRes.getData() != null) ? svRes.getData() : List.of();
        if (services.isEmpty()) {
            // clear old quote if any
            session.removeAttribute("shippingQuote");
            return ResponseEntity.ok(Map.of(
                    "services", services,
                    "fee", 0,
                    "serviceId", null,
                    "subtotal", cartService.getSubtotal(session),
                    "total", cartService.getSubtotal(session)
            ));
        }

        // Chọn serviceId đầu tiên (tuỳ bạn có thể lọc)
        int serviceId = services.get(0).getServiceId();

        // 2) Khối lượng/giá trị bảo hiểm theo giỏ hiện tại
        int qty = Math.max(1, cartService.getTotalQuantity(session));
        BigDecimal cartSubtotal = cartService.getSubtotal(session);
        int weight = qty * defaultItemWeight;

        // 3) Tính phí
        FeeRes feeRes = ghn.calculateFee(
                fromDistrictId, toDistrictId, toWardCode,
                serviceId, weight, defLen, defWid, defHei,
                cartSubtotal.intValue()
        );
        int fee = (feeRes != null && feeRes.getData() != null && feeRes.getData().getTotal() != null)
                ? feeRes.getData().getTotal() : 0;

        // 4) LƯU VÀO SESSION để dùng khi submit
        Map<String, Object> quote = new HashMap<>();
        quote.put("serviceId", serviceId);
        quote.put("fee", fee);
        quote.put("toDistrictId", toDistrictId);
        quote.put("toWardCode", toWardCode);
        quote.put("addressId", addressId);
        session.setAttribute("shippingQuote", quote);

        // 5) Trả về cho UI
        return ResponseEntity.ok(Map.of(
                "services", services,
                "serviceId", serviceId,
                "fee", fee,
                "subtotal", cartSubtotal,
                "total", cartSubtotal.add(BigDecimal.valueOf(fee))
        ));
    }
}
