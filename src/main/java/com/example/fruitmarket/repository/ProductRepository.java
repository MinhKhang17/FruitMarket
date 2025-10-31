package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Product;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
