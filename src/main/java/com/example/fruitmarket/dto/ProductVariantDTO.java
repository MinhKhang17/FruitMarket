package com.example.fruitmarket.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data

public class ProductVariantDTO {
    private String variantName;
    private long stock;
    private BigDecimal price;
}
