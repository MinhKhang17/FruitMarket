package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    Product findProductById(Long id);
}
