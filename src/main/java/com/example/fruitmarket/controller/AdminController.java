package com.example.fruitmarket.controller;

import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private ProductService productService;
    @Autowired private BrandsService brandsService;
    @Autowired private CategorysService categorysService;
    @Autowired private VariantService variantService;
    @Autowired private ImageService imageService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminController.class);

    @GetMapping({"", "/adminPage"})
    public String adminPage(Model model, HttpSession session) {
        Object logged = (session != null) ? session.getAttribute("loggedUser") : null;
        String username = "admin";
        if (logged instanceof com.example.fruitmarket.model.Users user) {
            username = user.getUsername();
        }
        model.addAttribute("loggedUserName", username);
        return "admin/adminPage";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categorysService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/create")
    public String createCategoryForm(Model model) {
        model.addAttribute("category", new Categorys());
        return "admin/createCategory";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Categorys category) {
        categorysService.addCategorys(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/brands")
    public String brands(Model model) {
        model.addAttribute("brands", brandsService.findAll());
        return "admin/brands";
    }

    @GetMapping("/brands/create")
    public String createBrandForm(Model model) {
        model.addAttribute("brand", new Brands());
        return "admin/createBrand";
    }

    @PostMapping("/brands/save")
    public String saveBrand(@ModelAttribute Brands brand) {
        brandsService.addBrand(brand);
        return "redirect:/admin/brands";
    }

    @GetMapping("/products")
    public String adminProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/create")
    public String createProduct(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categorysService.findAll());
        model.addAttribute("brands", brandsService.findAll());
        return "admin/createProduct";
    }

    // ✅ POST mapping gọn gàng, thống nhất
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product) {
        productService.saveProduct(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categorysService.findAll());
        model.addAttribute("brands", brandsService.findAll());
        return "admin/createProduct";
    }

    @PostMapping("/products/update")
    public String updateProduct(@ModelAttribute Product product) {
        productService.saveProduct(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
        return "redirect:/admin/products";
    }

    /* ---------------------------- VARIANTS ---------------------------- */
    @GetMapping("/products/{id}/variants")
    public String showVariantPage(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "admin/productVariant";
    }

    @PostMapping("/products/{productId}/variant/save")
    public String saveVariant(@PathVariable Long productId,
                              @ModelAttribute ProductVariant variant,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files)
            throws IOException {

        // Chỉ upload ảnh đầu tiên (One-to-One)
        variantService.createVariant(productId, variant, files, ImageType.PRODUCT_VARIANT);
        return "redirect:/admin/products/" + productId + "/variants";
    }
}
