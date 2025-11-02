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
import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.service.OrderService;
import com.example.fruitmarket.enums.OrderStauts;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    @Autowired private OrderService orderService;

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

    @GetMapping("/orders")
    public String adminOrders(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String viewOrderDetail(@PathVariable Long id, Model model) {
        try {
            Order order = orderService.getOrderById(id);
            if (order == null) {
                model.addAttribute("error", "Không tìm thấy đơn hàng với ID: " + id);
                return "redirect:/admin/orders";
            }
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", OrderStauts.values());
            return "admin/orderDetail";
        } catch (Exception e) {
            log.error("Error loading order detail: ", e);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/orders/{id}/updateStatus")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("status") String status,
                                    RedirectAttributes ra) {
        try {
            Order order = orderService.getOrderById(id);
            if (order == null) {
                ra.addFlashAttribute("message", "Không tìm thấy đơn hàng!");
                ra.addFlashAttribute("type", "danger");
                return "redirect:/admin/orders";
            }
            order.setOrderStauts(OrderStauts.valueOf(status));
            orderService.updateOrder(order);
            ra.addFlashAttribute("message", "Cập nhật trạng thái đơn hàng thành công!");
            ra.addFlashAttribute("type", "success");
        } catch (Exception e) {
            log.error("Error updating order status: ", e);
            ra.addFlashAttribute("message", "Lỗi: " + e.getMessage());
            ra.addFlashAttribute("type", "danger");
        }
        return "redirect:/admin/orders/" + id;
    }
}
