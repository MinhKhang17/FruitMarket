package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Province;
import org.springframework.stereotype.Service;

@Service
public interface ProvinceService {

    Province findByProvinceId(Integer provinceId);
}
