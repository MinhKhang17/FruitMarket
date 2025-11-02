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

@Controller()
@RequestMapping("/admin")
public class AdminController {
    @Autowired private ProductService productService;
    @Autowired private BrandsService brandsService;
    @Autowired private CategorysService categorysService;
    @Autowired private ImageService imageService;
    @Autowired private VariantService variantService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminController.class);

    @GetMapping({"/adminPage", ""})
    public String adminPage(Model model, HttpSession session){
        Object logged = (session != null) ? session.getAttribute("loggedUser") : null;
        if (logged instanceof com.example.fruitmarket.model.Users) {
            try {
                model.addAttribute("loggedUserName", ((com.example.fruitmarket.model.Users) logged).getUsername());
            } catch (Exception e) {
                model.addAttribute("loggedUserName", "admin");
            }
        } else {
            model.addAttribute("loggedUserName", "admin");
        }
        return "admin/adminPage";
    }

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
    public String adminProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/create")
    public String createProduct(Model model){
        try {
            // always put a Product instance in model (avoids template needing to create one)
            model.addAttribute("product", new Product());
            model.addAttribute("categories", categorysService.findAll());
            model.addAttribute("brands", brandsService.findAll());
            return "admin/createProduct";
        } catch (Exception ex) {
            log.error("Error preparing create product page", ex);
            // optional: pass a friendly message to a general error page or redirect back
            model.addAttribute("errorMessage", "Có lỗi khi tải trang tạo sản phẩm: " + ex.getMessage());
            return "admin/createProduct"; // vẫn trả template nhưng bạn sẽ thấy errorMessage nếu cần
        }
    }


    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam(value = "productImage", required = false) List<MultipartFile> files)
            throws IOException {
        Product saved = productService.saveProduct(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
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

    // Brands & Categories handling (kept minimal)
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

    @GetMapping("/products/{id}/variants")
    public String showVariantPage(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "admin/productVariants";
    }

    @PostMapping("/products/{productId}/variant/save")
    public String saveVariant(@PathVariable Long productId,
                              @ModelAttribute ProductVariant variant,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files)
            throws IOException {

        variantService.createVariant(productId, variant, files, ImageType.PRODUCT_VARIANT);
        return "redirect:/admin/products/" + productId + "/variants";
    }
}
