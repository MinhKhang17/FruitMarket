package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Product;

import java.util.List;

public interface ProductService {
    Product saveProduct(Product product);

    List<Product> findAll();
}
