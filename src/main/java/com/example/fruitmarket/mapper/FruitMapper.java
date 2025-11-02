package com.example.fruitmarket.mapper;

import com.example.fruitmarket.dto.ProductCheckoutResponse;
import com.example.fruitmarket.dto.ProductDTO;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;

public class FruitMapper {
    public static ProductDTO toProductDTO(Product product) {
        ProductDTO result = new ProductDTO();
        result.setProductId(product.getId());
        result.setProductName(product.getProductName());
        result.setBrandName(product.getBrand().getName());
        result.setCategoryName(product.getCategory().getName());
        result.setProductPrice(product.getVariants().get(0).getPrice());
        result.setDescription(product.getProduct_description());
        result.setProductVariants(product.getVariants());
        if (product.getVariants().get(0) != null && product.getVariants().get(0).getImage() != null) {

        result.setImageUrl(product.getVariants().get(0).getImage().getUrl());
    }
return result;
    }

    public static ProductCheckoutResponse toProductCheckout(ProductVariant productVariant) {
        ProductCheckoutResponse result = new ProductCheckoutResponse();
        result.setId(productVariant.getId());
        result.setProductName(productVariant.getProduct().getProductName());
        result.setProduct_description(productVariant.getProduct().getProduct_description());
        result.setProduct_price(productVariant.getPrice());

        if(productVariant.getImage() != null){
            result.setImage_url(productVariant.getImage().getUrl());
        }
        return  result;
    }
}
