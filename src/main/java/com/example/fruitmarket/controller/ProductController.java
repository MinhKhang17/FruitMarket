package com.example.fruitmarket.controller;

import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
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
import java.util.Map;

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

    @GetMapping("/products/home-component")
    public String productsComponent(Model model) {
        // 1) latest - top 12 newest
        List<Product> latest = productService.findLatestProducts(12);
        model.addAttribute("latestProducts", latest);

        // 2) categories - load all categories (or top N) and for each category a small list of products
        List<Categorys> categories = categorysService.findAll();
        model.addAttribute("categories", categories);
        Map<Long, List<Product>> categoryProducts = productService.findProductsGroupedByCategory(categories, 6); // map: categoryId -> list
        model.addAttribute("categoryProducts", categoryProducts);

        // 3) brands - load brands and for each brand a small list
        List<Brands> brands = brandsService.findAll();
        model.addAttribute("brands", brands);
        Map<Long, List<Product>> brandProducts = productService.findProductsGroupedByBrand(brands, 4);
        model.addAttribute("brandProducts", brandProducts);

        return "home/product_component";
    }
    @GetMapping("/products/{id}/quick")
    public String quickView(@PathVariable Long id, Model model) {
        Product p = productService.findById(id);
        model.addAttribute("p", p);
        return "home/partials/product_quick"; // partial HTML
    }


}
