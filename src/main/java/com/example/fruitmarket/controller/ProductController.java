// java
// file: src/main/java/com/example/fruitmarket/controller/ProductController.java
package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    public String listProducts(Model model) {
        model.addAttribute("products", productRepository.findAll());
        return "home/product";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        return productRepository.findById(id)
                .map(p -> { model.addAttribute("product", p); return "home/detail"; })
                .orElse("redirect:/products");
    }
}
