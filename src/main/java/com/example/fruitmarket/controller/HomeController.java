package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import com.example.fruitmarket.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CategorysService categorysService;
    private final BrandsService brandsService;

    @Autowired private ProductService productService;
        @GetMapping({"/", "/home"})
        public String homePage(Model model) {
            // 1) Latest products (ví dụ top 8)
            List<Product> latest = productService.findLatestProducts(8);
            model.addAttribute("latestProducts", latest);

            // 2) Categories + small list per category (ví dụ 6 mỗi category)
            List<Categorys> categories = categorysService.findAll();
            model.addAttribute("categories", categories);
            Map<Long, List<Product>> categoryProducts = productService.findProductsGroupedByCategory(categories, 6);
            model.addAttribute("categoryProducts", categoryProducts);

            // 3) Brands + small list per brand (ví dụ 4 mỗi brand)
            List<Brands> brands = brandsService.findAll();
            model.addAttribute("brands", brands);
            Map<Long, List<Product>> brandProducts = productService.findProductsGroupedByBrand(brands, 4);
            model.addAttribute("brandProducts", brandProducts);

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
