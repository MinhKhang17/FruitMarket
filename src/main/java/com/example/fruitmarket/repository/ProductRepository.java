package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    Product findProductById(Long id);

    boolean existsByProductName(String productName);

    List<Product> findAllByCreatedAt(LocalDateTime createdAt, Sort sort);

    List<Product> findAllByOrderByCreatedAtDesc(PageRequest of);

    List<Product> findTopByCategoryIdOrderByCreatedAtDesc(Long id, PageRequest of);

    List<Product> findTopByBrandIdOrderByCreatedAtDesc(Long id, PageRequest of);
}
