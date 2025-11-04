package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepo extends JpaRepository<Province, Integer> {}
