package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Categorys;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CategorysService {
    Categorys findById(long id);
    Categorys addCategorys(Categorys categorys);
    Categorys updateCategorys(Categorys categorys);
    void deleteCategorys(Categorys categorys);
    List<Categorys> findAll();
}
