package com.example.fruitmarket.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
public class OrderRequest {
    private Long addressId;
    private String paymentMethod;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal totalPrice;
    private Integer totalQuantity;

    // Thông tin cho GHN/địa chỉ chi tiết
    private Integer toDistrictId;
    private String toWardCode;

    // Mở rộng để lưu phí & dịch vụ GHN nếu cần
    private BigDecimal shippingFee;
    private Integer serviceId;

    @Data
    public static class OrderItem {
        private Long productId;
        private Long variantId;
        private String name;
        private BigDecimal price;
        private Integer quantity;

        // Tuỳ chọn: cân nặng từng item (nếu có)
        private Double weight;
        private Units unit;
    }
}

