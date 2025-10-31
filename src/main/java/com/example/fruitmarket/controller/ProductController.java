package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import com.example.fruitmarket.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProductController {
    @Autowired private CategorysService categorysService;
    @Autowired private BrandsService brandsService;
    @Autowired private ProductService productService;

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categorysService.findAll());
        return "admin/categories";
    }
    @GetMapping("/brands")
    public String brands(Model model) {
        model.addAttribute("brands", brandsService.findAll());
        return "admin/brands";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }


        @GetMapping("/createProduct")
    public String createProduct(Model model){
        model.addAttribute("product", new Product());
        model.addAttribute("categories",categorysService.findAll() );
        model.addAttribute("brands",brandsService.findAll() );
        return "admin/createProduct";
    }
    @PostMapping("/admin/products/save")
    public String saveProduct(@ModelAttribute Product product, BindingResult result, HttpSession session){
        productService.saveProduct(product);
        return "redirect:/products";
    }
}
