package com.example.fruitmarket.dto;

import com.example.fruitmarket.enums.Units;
import com.example.fruitmarket.model.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {
    private Long productId;
    private String productName;
    private String description;
    private String imageUrl = "/images/placeholder.png";               // url của ảnh (nếu có)
    private String categoryName;
    private String brandName;
    private BigDecimal productPrice;       // fallback price (nếu không dùng variant)
    private List<ProductVariant> productVariants;
    private Units unit;



}
