package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.ProductDTO;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);

    List<Product> findAll();

    Product findById(Long id);
    void deleteById(Long id);

    ProductDTO findAllProductWithProductVariant(long id);

    ProductVariant findProductVariantById(long productVariantId);

    void decreaseStock(Long variantId, Integer quantity);

}
