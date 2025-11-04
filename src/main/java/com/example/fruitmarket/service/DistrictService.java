package com.example.fruitmarket.service;

import com.example.fruitmarket.model.District;
import org.springframework.stereotype.Service;

@Service
public interface DistrictService {
    District findByDistrictId(Integer findByDistrictId);
}
