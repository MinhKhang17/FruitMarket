package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Categorys;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategorysRepository extends JpaRepository<Categorys, Long> {
    Categorys findByName(String name);
    Categorys findById(long id);
}
