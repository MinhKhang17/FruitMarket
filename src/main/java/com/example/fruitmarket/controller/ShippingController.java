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

    public static record QuoteReq(
            Long addressId,
            Integer toDistrictId,
            String toWardCode,
            Integer qty,
            Integer weight,
            Integer length, Integer width, Integer height,
            Integer insuranceValue
    ) {}

    @PostMapping("/quote")
    public ResponseEntity<?> quoteByPayload(@RequestBody QuoteReq req, HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) return ResponseEntity.status(401).build();

        // 1) Xác định toDistrictId / toWardCode
        Integer toDistrictId = req.toDistrictId();
        String  toWardCode   = req.toWardCode();

        if (toDistrictId == null || toWardCode == null) {
            // nếu client gửi addressId thì tra cứu
            if (req.addressId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Thiếu toDistrictId/toWardCode hoặc addressId"));
            }
            User_detail addr = userService.findUserDetalById(req.addressId());
            if (addr == null || addr.getDistrict() == null || addr.getWard() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Địa chỉ thiếu district/ward"));
            }
            toDistrictId = addr.getDistrict().getDistrictId();
            toWardCode   = addr.getWard().getWardCode();
        }

        // 2) Lấy dịch vụ
        AvailableServicesRes svRes = ghn.availableServices(fromDistrictId, toDistrictId);
        List<AvailableServicesRes.ServiceItem> services =
                (svRes != null && svRes.getData() != null) ? svRes.getData() : List.of();
        if (services.isEmpty()) {
            session.removeAttribute("shippingQuote");
            BigDecimal subtotal = cartService.getSubtotal(session);
            return ResponseEntity.ok(Map.of(
                    "services", services,
                    "fee", 0,
                    "serviceId", null,
                    "subtotal", subtotal,
                    "total", subtotal
            ));
        }
        int serviceId = services.get(0).getServiceId(); // hoặc chọn theo logic rẻ nhất

        // 3) Đồng bộ số liệu với UI nếu có
        int qty = (req.qty() != null) ? Math.max(1, req.qty()) : Math.max(1, cartService.getTotalQuantity(session));
        int weight = (req.weight() != null && req.weight() > 0)
                ? req.weight()
                : qty * defaultItemWeight;

        int len = (req.length() != null) ? req.length() : defLen;
        int wid = (req.width()  != null) ? req.width()  : defWid;
        int hei = (req.height() != null) ? req.height() : defHei;

        BigDecimal subtotal = (req.insuranceValue() != null)
                ? BigDecimal.valueOf(req.insuranceValue())
                : cartService.getSubtotal(session);

        // 4) Tính phí
        FeeRes feeRes = ghn.calculateFee(
                fromDistrictId, toDistrictId, toWardCode,
                serviceId, weight, len, wid, hei,
                subtotal.intValue()
        );
        int fee = (feeRes != null && feeRes.getData() != null && feeRes.getData().getTotal() != null)
                ? feeRes.getData().getTotal() : 0;

        // 5) Lưu session để submit dùng đúng y số này
        Map<String, Object> quote = new HashMap<>();
        quote.put("serviceId", serviceId);
        quote.put("fee", fee);
        quote.put("toDistrictId", toDistrictId);
        quote.put("toWardCode", toWardCode);
        session.setAttribute("shippingQuote", quote);

        return ResponseEntity.ok(Map.of(
                "services", services,
                "serviceId", serviceId,
                "fee", fee,
                "subtotal", subtotal,
                "total", subtotal.add(BigDecimal.valueOf(fee))
        ));
    }

    @GetMapping("/quote")
    public ResponseEntity<?> quoteGet(@RequestParam Long addressId, HttpSession session) {
        // Dựng payload tối thiểu, có thể thêm qty/weight/dims nếu muốn
        QuoteReq req = new QuoteReq(
                addressId,
                null, null,
                null, null,  // qty, weight -> để handler POST tự suy
                null, null, null, // length,width,height
                null          // insuranceValue
        );
        return quoteByPayload(req, session); // gọi lại handler POST
    }
}
