package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.repository.CategorysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorysServiceImpl implements  CategorysService {

    private final CategorysRepository categorysRepository;

    @Override
    public Categorys findById(long id) {
        return categorysRepository.findById(id);
    }

    @Override
    public Categorys addCategorys(Categorys categorys) {
        return categorysRepository.save(categorys);
    }

    @Override
    public List<Categorys> findAll() {
        return categorysRepository.findAll();
    }
}
