package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.District;
import com.example.fruitmarket.model.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepo extends JpaRepository<District, Integer> {
    List<District> findByProvinceOrderByDistrictNameAsc(Province province);
}
