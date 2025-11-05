package com.example.fruitmarket.dto;

import com.example.fruitmarket.enums.Units;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class OrderRequest {
    private Long addressId;
    private String paymentMethod;
    private List<OrderItem> items = new ArrayList<>();
    private BigDecimal totalPrice;
    private Integer totalQuantity;

    @Data
    public static class OrderItem {
        private Long productId;
        private Long variantId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
        private Double weight;
        private Units unit;
    }
}

