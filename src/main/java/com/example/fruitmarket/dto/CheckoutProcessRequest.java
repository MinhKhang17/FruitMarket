package com.example.fruitmarket.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutProcessRequest {
    private Long variantId;        // ID biến thể sản phẩm
    private Integer quantity;      // Số lượng mua
    private Long addressId;        // ID địa chỉ giao hàng
    private String paymentMethod;  // Phương thức thanh toán: COD, BANK, MOMO...+
    private Integer serviceId;
    private Integer toDistrictId;
    private String toWardCode;
    private BigDecimal shippingFee;
 }
