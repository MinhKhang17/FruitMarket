package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant,Long > {
    Long id(Long id);
}
