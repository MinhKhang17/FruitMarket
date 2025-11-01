package com.example.fruitmarket.Dto;

import lombok.Data;

@Data
public class CheckoutForm {
    private Long variantId;
    private Integer quantity;
    private Long addressId;
    private String paymentMethod;
}
