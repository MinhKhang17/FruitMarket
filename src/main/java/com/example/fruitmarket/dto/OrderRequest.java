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

    public void setAddressId(Long addressId) { this.addressId = addressId; }

    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public void setItems(List<OrderItem> items) { this.items = items; }

    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    @Getter
    public static class OrderItem {
        private Long productId;
        private Long variantId;
        private String name;
        private java.math.BigDecimal price;
        private Integer quantity;
        private Double weight;

        public void setProductId(Long productId) { this.productId = productId; }

        public void setVariantId(Long variantId) { this.variantId = variantId; }

        public void setName(String name) { this.name = name; }

        public void setPrice(java.math.BigDecimal price) { this.price = price; }

        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public void setWeight(Double weight) { this.weight = weight; }
    }
}

