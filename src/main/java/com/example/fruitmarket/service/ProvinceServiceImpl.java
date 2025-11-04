package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Province;
import com.example.fruitmarket.repository.DistrictRepo;
import com.example.fruitmarket.repository.ProvinceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProvinceServiceImpl implements  ProvinceService {

    private final ProvinceRepo provinceRepo;

    @Override
    public Province findByProvinceId(Integer provinceId) {
        return provinceRepo.findById(provinceId).orElse(null);
    }
}
