package com.example.fruitmarket.controller;

import ch.qos.logback.core.model.Model;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import com.example.fruitmarket.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller()
@RequestMapping("/admin")
public class AdminController {
    @Autowired private ProductService productService;
    @Autowired private BrandsService brandsService;
    @Autowired private CategorysService categorysService;
    @GetMapping("/adminPage")
    public  String adminPage(Model model){
        return "admin/adminPage";
    }

    @GetMapping("/categories")
    public String categories(org.springframework.ui.Model model) {
        model.addAttribute("categories", categorysService.findAll());
        return "admin/categories";
    }

    @GetMapping("/brands")
    public String brands(org.springframework.ui.Model model) {
        model.addAttribute("brands", brandsService.findAll());
        return "admin/brands";
    }

    // Admin: product list & create (NOTE: admin path starts with /admin)
    @GetMapping("/products")
    public String adminProducts(org.springframework.ui.Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/product/create")
    public String createProduct(org.springframework.ui.Model model){
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categorysService.findAll());
        model.addAttribute("brands", brandsService.findAll());
        return "admin/createProduct";
    }

    @PostMapping("/product/save")
    public String saveProduct(@ModelAttribute Product product, BindingResult result, HttpSession session){
        productService.saveProduct(product);
        return "redirect:/admin/products";
    }


}
