package com.example.fruitmarket.Dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private long product_variant_id;
    private int quantity;
}
