package com.example.fruitmarket.Dto;

import lombok.Data;

@Data
public class CheckoutProcessRequest {
    private Long variantId;        // ID biến thể sản phẩm
    private Integer quantity;      // Số lượng mua
    private Long addressId;        // ID địa chỉ giao hàng
    private String paymentMethod;  // Phương thức thanh toán: COD, BANK, MOMO...
 }
