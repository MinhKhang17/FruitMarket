package com.example.fruitmarket.service;

import com.example.fruitmarket.model.District;
import com.example.fruitmarket.repository.DistrictRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DistrictServiceImpl implements DistrictService {

    private final DistrictRepo districtRepo;

    @Override
    public District findByDistrictId(Integer findByDistrictId) {
        return districtRepo.findById(findByDistrictId).orElse(null);
    }
}
