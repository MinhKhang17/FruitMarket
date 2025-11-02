package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long product_variant_id;
    private int quantity;
}
