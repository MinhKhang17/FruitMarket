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

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class  ProductController {

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
    public String listProducts(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long brand,
            @RequestParam(required = false) String q,
            Model model
    ) {
        List<Product> products;

        // Nếu có filter category hoặc brand
        if (category != null || brand != null) {
            products = productService.findByFilters(category, brand);
        } else {
            // Lấy tất cả products
            products = productService.findAll();

            // Lọc chỉ lấy products có variants
            products = products.stream()
                    .filter(p -> p.getVariants() != null && !p.getVariants().isEmpty())
                    .collect(Collectors.toList());
        }

        // Lọc theo search query nếu có
        if (q != null && !q.trim().isEmpty()) {
            String query = q.toLowerCase().trim();
            products = products.stream()
                    .filter(p -> p.getProduct_name().toLowerCase().contains(query) ||
                            (p.getProduct_description() != null &&
                                    p.getProduct_description().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("products", products);

        // Debug log
        System.out.println("Filter - Category: " + category + ", Brand: " + brand + ", Query: " + q);
        System.out.println("Products found: " + products.size());

        return "home/product";
    }




}
