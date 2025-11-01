package com.example.fruitmarket.service;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.fruitmarket.model.Product;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);

    List<Product> findAll();

    Product findById(Long id);
}
