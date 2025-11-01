package com.example.fruitmarket.controller;

import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import com.example.fruitmarket.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class ProductController {

    private final CategorysService categorysService;
    private final BrandsService brandsService;
    private final ProductService productService;




    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        ProductDTO product = productService.findAllProductWithProductVariant(id);
        model.addAttribute("product", product);
        return "home/detail";
    }


    @GetMapping("/products")
    public String listProducts(Model model, HttpSession session) {
        model.addAttribute("products", productService.findAll());
        return "home/product";
    }



}
