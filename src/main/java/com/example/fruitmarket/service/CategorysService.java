package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Categorys;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CategorysService {
    Categorys getById(long id);
    Categorys addCategorys(Categorys categorys);
    List<Categorys> findAll();
    void deleteById(Long id);
    Categorys findByName(String name);
}
