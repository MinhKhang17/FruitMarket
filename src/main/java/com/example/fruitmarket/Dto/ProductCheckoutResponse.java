package com.example.fruitmarket.Dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCheckoutResponse {
    private Long id;
    private String product_name;
    private String product_description;
    private BigDecimal product_price;
    private String image_url;
}
