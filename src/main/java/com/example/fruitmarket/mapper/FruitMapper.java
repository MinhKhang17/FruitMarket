package com.example.fruitmarket.mapper;

import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.model.Product;

public class FruitMapper {
    public static ProductDTO toProductDTO(Product product) {
        ProductDTO result = new ProductDTO();
        result.setProductId(product.getId());
        result.setProductName(product.getProduct_name());
        result.setBrandName(product.getBrand().getName());
        result.setCategoryName(product.getCategory().getName());
        result.setProductPrice(product.getVariants().get(0).getPrice());
        result.setDescription(product.getProduct_description());
        if (product.getVariants().get(0) != null && product.getVariants().get(0).getImage() != null) {

        result.setImageUrl(product.getVariants().get(0).getImage().getUrl());
    }
return result;
    }
}
