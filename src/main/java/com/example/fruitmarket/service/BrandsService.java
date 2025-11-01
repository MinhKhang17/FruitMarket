package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Brands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BrandsService {
    Brands findByName(String name);
    Brands addBrand(Brands brand);
    Brands findById(Long id);
    List<Brands> findAll();
    void deleteById(Long id);
}
