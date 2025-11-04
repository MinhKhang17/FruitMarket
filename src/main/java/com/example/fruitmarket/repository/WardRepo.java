package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.District;
import com.example.fruitmarket.model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepo extends JpaRepository<Ward, String> {
    List<Ward> findByDistrictOrderByWardNameAsc(District district);
}