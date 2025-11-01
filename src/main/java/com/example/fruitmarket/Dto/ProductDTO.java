package com.example.fruitmarket.Dto;

import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    private Long productId;
    private String productName;
    private String description;
    private String imageUrl = "/images/placeholder.png";               // url của ảnh (nếu có)
    private String categoryName;
    private String brandName;
    private BigDecimal productPrice;       // fallback price (nếu không dùng variant)
    private List<ProductVariant> productVariants;



}
