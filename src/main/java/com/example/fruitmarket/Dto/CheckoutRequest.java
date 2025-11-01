package com.example.fruitmarket.Dto;

import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    private List<Long> product_variant_id;
    private int quantity;
}
