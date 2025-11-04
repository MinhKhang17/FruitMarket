package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Ward;
import org.springframework.stereotype.Service;

@Service
public interface WardService {
    Ward findByWardCode(String wardCode);
}
