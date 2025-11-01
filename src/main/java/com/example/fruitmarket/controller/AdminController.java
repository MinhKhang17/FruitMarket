package com.example.fruitmarket.controller;

import com.example.fruitmarket.Enums.ImageType;
import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.service.BrandsService;
import com.example.fruitmarket.service.CategorysService;
import com.example.fruitmarket.service.ImageService;
import com.example.fruitmarket.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller()
@RequestMapping("/admin")
public class AdminController {
    @Autowired private ProductService productService;
    @Autowired private BrandsService brandsService;
    @Autowired private CategorysService categorysService;
    @Autowired private ImageService imageService;
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
    public String saveProduct(@ModelAttribute Product product,
                              BindingResult result,
                              HttpSession session,
                              @RequestParam(value = "productImage", required = false) List<MultipartFile> files)
            throws IOException {

        Product saved = productService.saveProduct(product);

        if (files != null && !files.isEmpty()) {
            imageService.uploadImagesForProduct(saved.getId(), files, ImageType.PRODUCT);
        }

        return "redirect:/admin/products";
    }

    // Xóa sản phẩm
    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, org.springframework.ui.Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categorysService.findAll());
        model.addAttribute("brands", brandsService.findAll());
        return "admin/createProduct"; // dùng lại form createProduct cho edit
    }

    @PostMapping("/product/update")
    public String updateProduct(@ModelAttribute Product product, BindingResult result) {
        if (result.hasErrors()) {
            return "admin/createProduct";
        }
        productService.saveProduct(product); // cùng method với saveProduct, vì JPA .save() sẽ update nếu có id
        return "redirect:/admin/products";
    }

    @GetMapping("/brands/create")
    public String createBrandForm(Model model) {
        model.addAttribute("brand", new Brands());
        return "admin/createBrand";
    }

    @PostMapping("/brands/save")
    public String saveBrand(@ModelAttribute("brand") Brands brand, BindingResult result) {
        if (result.hasErrors()) return "admin/createBrand";
        brandsService.addBrand(brand);
        return "redirect:/admin/brands";
    }

    @GetMapping("/brands/edit/{id}")
    public String editBrand(@PathVariable Long id, Model model) {
        Brands brand = brandsService.findById(id);
        if (brand == null) return "redirect:/admin/brands";
        model.addAttribute("brand", brand);
        return "admin/createBrand";
    }

    @PostMapping("/brands/update")
    public String updateBrand(@ModelAttribute("brand") Brands brand, BindingResult result) {
        if (result.hasErrors()) return "admin/createBrand";
        brandsService.addBrand(brand);
        return "redirect:/admin/brands";
    }

    @GetMapping("/brands/delete/{id}")
    public String deleteBrand(@PathVariable Long id) {
        brandsService.deleteById(id); // bạn thêm method này vào BrandsServiceImpl
        return "redirect:/admin/brands";
    }

    @GetMapping("/categories/create")
    public String createCategoryForm(Model model) {
        model.addAttribute("category", new Categorys());
        return "admin/createCategory";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("category") Categorys category, BindingResult result) {
        if (result.hasErrors()) return "admin/createCategory";
        categorysService.addCategorys(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Long id, Model model) {
        Categorys category = categorysService.findById(id);
        if (category == null) return "redirect:/admin/categories";
        model.addAttribute("category", category);
        return "admin/createCategory";
    }

    @PostMapping("/categories/update")
    public String updateCategory(@ModelAttribute("category") Categorys category, BindingResult result) {
        if (result.hasErrors()) return "admin/createCategory";
        categorysService.addCategorys(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categorysService.deleteById(id);
        return "redirect:/admin/categories";
    }

}
