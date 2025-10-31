package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.repository.BrandsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandsServiceImpl implements BrandsService {

    private final BrandsRepository brandsRepository;

    @Override
    public Brands findByName(String name) {
        return brandsRepository.findByName(name);
    }

    @Override
    public Brands addBrand(Brands brand) {
        return brandsRepository.save(brand);
    }

    @Override
    public Brands findById(Long id) {
        return brandsRepository.findById(id).orElse(null);
    }

    @Override
    public List<Brands> findAll() {
        return brandsRepository.findAll();
    }
}
