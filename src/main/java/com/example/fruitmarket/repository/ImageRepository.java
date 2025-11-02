package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
