package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {
    private static final Logger log = LoggerFactory.getLogger(GlobalModelAttributes.class);

    private final CategorysService categorysService;
    private final BrandsService brandsService;

    @PostConstruct
    public void init() {
        log.info("GlobalModelAttributes created; categoryService={}, brandService={}",
                categorysService != null, brandsService != null);
        try {
            List<Categorys> c = categorysService.findAll();
            List<Brands> b = brandsService.findAll();
            log.info("Sample fetch sizes: categories={}, brands={}", c == null ? 0 : c.size(), b == null ? 0 : b.size());
        } catch (Throwable ex) {
            log.error("Error while calling services from GlobalModelAttributes.init()", ex);
        }
    }

    @ModelAttribute("categories")
    public List<Categorys> categories() {
        try {
            return categorysService.findAll();
        } catch (Throwable ex) {
            log.warn("Failed to load categories for header, returning empty list", ex);
            return List.of();
        }
    }

    @ModelAttribute("brands")
    public List<Brands> brands() {
        try {
            return brandsService.findAll();
        } catch (Throwable ex) {
            log.warn("Failed to load brands for header, returning empty list", ex);
            return List.of();
        }
    }
}
