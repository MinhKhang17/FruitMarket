package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Brands;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandsRepository extends JpaRepository<Brands, Long> {
    Brands findByName(String name);

}
