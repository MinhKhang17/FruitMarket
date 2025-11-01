package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeController {

    private final CategorysService categorysService;
    private final BrandsService brandsService;

    @GetMapping("/")
    public String homePage() {
        return "home/page";
    }


    @ModelAttribute("categories")
    public List<Categorys> categories() {
        return categorysService.findAll();
    }

    @ModelAttribute("brands")
    public List<Brands> brands() {
        return brandsService.findAll();
    }
}
