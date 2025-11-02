package com.example.fruitmarket.service;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import org.springframework.ui.Model;

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
