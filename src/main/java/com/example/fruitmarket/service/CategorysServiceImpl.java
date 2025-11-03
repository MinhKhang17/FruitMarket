package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.repository.CategorysRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorysServiceImpl implements CategorysService {

    private final CategorysRepository categorysRepository;

    @Override
    public Categorys getById(long id) {
        return categorysRepository.findById(id).orElse(null);
    }

    @Override
    public Categorys addCategorys(Categorys categorys) {
        if (categorys.getId() == null) {
            categorys.setStatus(true);
        }
        return categorysRepository.save(categorys);
    }

    @Override
    public List<Categorys> findAll() {
        return categorysRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        Categorys category = categorysRepository.findById(id).orElse(null);
        if (category != null) {
            category.setStatus(false);
            categorysRepository.save(category);
        }
    }
}
