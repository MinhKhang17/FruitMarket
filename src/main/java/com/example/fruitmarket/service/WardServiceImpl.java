package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Ward;
import com.example.fruitmarket.repository.WardRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WardServiceImpl implements  WardService {

    private final WardRepo wardRepo;

    @Override
    public Ward findByWardCode(String wardCode) {
        return wardRepo.findById(wardCode).orElse(null);
    }
}
