package com.example.fruitmarket.controller;

import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.enums.OrderStauts;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.enums.UserStatus;
import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.service.*;
import com.example.fruitmarket.util.AuthUtils;
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
    @Autowired
    private UserService userService;

    private String checkAdminAccess(HttpSession session) {
        if (!AuthUtils.isLoggedIn(session)) {
            return "redirect:/auth/login";
        }
        if (!AuthUtils.isAdmin(session)) {
            return "redirect:/access-denied";
        }
        return null;
    }

    @GetMapping({"", "/adminPage"})
    public String adminPage(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        Users logged = (Users) session.getAttribute("loggedUser");
        model.addAttribute("loggedUserName", logged != null ? logged.getUsername() : "admin");
        return "admin/adminPage";
    }

    @GetMapping("/categories")
    public String categories(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("categories", categorysService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        Categorys category = categorysService.getById(id);
        if (category == null) return "redirect:/admin/categories";
        model.addAttribute("category", category);
        return "admin/createCategory";
    }

    @GetMapping("/categories/create")
    public String createCategoryForm(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("category", new Categorys());
        return "admin/createCategory";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Categorys category,
                               RedirectAttributes ra,
                               HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;
        boolean isNew = (category.getId() == null);
        try {
            Categorys existing = categorysService.findByName(category.getName().trim());
            if (existing != null &&
                    (category.getId() == null || !existing.getId().equals(category.getId()))) {
                ra.addFlashAttribute("message", "Tên danh mục đã tồn tại!");
                ra.addFlashAttribute("type", "danger");
                return "redirect:/admin/categories";
            }
            categorysService.addCategorys(category);
            ra.addFlashAttribute("message",
                    isNew ? "Thêm danh mục mới thành công!" : "Cập nhật danh mục thành công!");
            ra.addFlashAttribute("type", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi khi lưu danh mục: " + e.getMessage());
            ra.addFlashAttribute("type", "danger");
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        Categorys category = categorysService.getById(id);
        if (category != null) {
            category.setStatus(false);
            categorysService.addCategorys(category);
            ra.addFlashAttribute("message", "Đã xóa danh mục (inactive).");
            ra.addFlashAttribute("type", "warning");
        } else {
            ra.addFlashAttribute("message", "Không tìm thấy danh mục.");
            ra.addFlashAttribute("type", "danger");
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/brands")
    public String brands(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("brands", brandsService.findAll());
        return "admin/brands";
    }

    @GetMapping("/brands/create")
    public String createBrandForm(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("brand", new Brands());
        return "admin/createBrand";
    }

    @GetMapping("/brands/edit/{id}")
    public String editBrandForm(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        Brands brand = brandsService.findById(id);
        if (brand == null) return "redirect:/admin/brands";

        model.addAttribute("brand", brand);
        return "admin/createBrand";
    }

    @PostMapping("/brands/save")
    public String saveBrand(@ModelAttribute Brands brand,
                            RedirectAttributes ra,
                            HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        try {
            boolean isNew = (brand.getId() == null);
            Brands existingBrand = brandsService.findByName(brand.getName().trim());
            if (existingBrand != null &&
                    (brand.getId() == null || !existingBrand.getId().equals(brand.getId()))) {
                ra.addFlashAttribute("message", "Tên thương hiệu đã tồn tại!");
                ra.addFlashAttribute("type", "danger");
                return "redirect:/admin/brands";
            }
            // Nếu brand mới (id null) => mặc định status = true (active)
            if (brand.getId() == null) {
                brand.setStatus(true);
            }
            brandsService.addBrand(brand);
            ra.addFlashAttribute("message",
                    isNew
                            ? "Thêm thương hiệu mới thành công!"
                            : "Cập nhật thương hiệu thành công!");
            ra.addFlashAttribute("type", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi khi lưu thương hiệu: " + e.getMessage());
            ra.addFlashAttribute("type", "danger");
        }
        return "redirect:/admin/brands";
    }

    @GetMapping("/brands/delete/{id}")
    public String deleteBrand(@PathVariable Long id,
                              RedirectAttributes ra,
                              HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        try {
            Brands brand = brandsService.findById(id);
            if (brand != null) {
                brand.setStatus(false); // inactive = xóa mềm
                brandsService.addBrand(brand);
                ra.addFlashAttribute("message", "Đã xóa thương hiệu (inactive).");
                ra.addFlashAttribute("type", "warning");
            } else {
                ra.addFlashAttribute("message", "Không tìm thấy thương hiệu.");
                ra.addFlashAttribute("type", "danger");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi khi xóa thương hiệu: " + e.getMessage());
            ra.addFlashAttribute("type", "danger");
        }
        return "redirect:/admin/brands";
    }

    @GetMapping("/products")
    public String adminProducts(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/create")
    public String createProduct(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("product", new Product());
        model.addAttribute("categories", categorysService.findAll());
        model.addAttribute("brands", brandsService.findAll());
        return "admin/createProduct";
    }

    // ✅ POST mapping gọn gàng, thống nhất
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product, HttpSession session, RedirectAttributes ra) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;
        try {
            boolean isNew = (product.getId() == null);
            productService.saveProduct(product);
            ra.addFlashAttribute("message",
                    isNew ? "Thêm sản phẩm mới thành công!" : "Cập nhật sản phẩm thành công!");
            ra.addFlashAttribute("type", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi khi lưu sản phẩm: " + e.getMessage());
            ra.addFlashAttribute("type", "danger");
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", categorysService.findAll());
        model.addAttribute("brands", brandsService.findAll());
        return "admin/editProduct";
    }

    @PostMapping("/products/update")
    public String updateProduct(@ModelAttribute Product product) {
        productService.saveProduct(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        productService.updateProductStatusToInactive(id);
        return "redirect:/admin/products";
    }

    @GetMapping("products/restore/{id}")
    public String restoreProduct(@PathVariable Long id, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        productService.updateProductStatusToActive(id);
        return "redirect:/admin/products";
    }

    /* ---------------------------- VARIANTS ---------------------------- */
    @GetMapping("/products/{id}/variants")
    public String showVariantPage(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "admin/productVariant";
    }

    @PostMapping("/products/{productId}/variant/save")
    public String saveVariant(@PathVariable Long productId,
                              @ModelAttribute ProductVariant variant,
                              @RequestParam(value = "files", required = false) List<MultipartFile> files,
                              HttpSession session)
            throws IOException {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        // Chỉ upload ảnh đầu tiên (One-to-One)
        variantService.createVariant(productId, variant, files, ImageType.PRODUCT_VARIANT);
        return "redirect:/admin/products/" + productId + "/variants";
    }

    @GetMapping("/productVariant/delete/{id}")
    public String deleteProductVariant(@PathVariable Long id) {
        ProductVariant variant = variantService.findById(id);
        Long productId = variant.getProduct().getId();

        variantService.updateStatusToInactive(id);

        return "redirect:/admin/products/" + productId + "/variants/list";
    }


    @GetMapping("/productVariant/edit/{id}")
    public String editProductVariant(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        ProductVariant variant = variantService.findById(id);
        model.addAttribute("variant", variant);
        model.addAttribute("product", variant.getProduct());
        return "admin/editVariant";
    }

    @PostMapping("/editVariant/update")
    public String updateProductVariant(@ModelAttribute ProductVariant variant,
                                       @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                       HttpSession session)
            throws IOException {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;
        variantService.update(variant, files, ImageType.PRODUCT_VARIANT);
        Long productId = variant.getProduct().getId();
        return "redirect:/admin/products/" + productId + "/variants/list";
    }

    @GetMapping("/productVariant/restore/{id}")
    public String restoreProductVariant(@PathVariable Long id) {
        ProductVariant variant = variantService.findById(id);
        Long productId = variant.getProduct().getId();

        variantService.updateStatusToActive(id);

        return "redirect:/admin/products/" + productId + "/variants/list";
    }

    @GetMapping("/products/{id}/variants/list")
    public String viewVariantList(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "admin/variantList";
    }



    @GetMapping("/orders")
    public String adminOrders(Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String viewOrderDetail(@PathVariable Long id, Model model, HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

        try {
            Order order = orderService.getOrderById(id);
            if (order == null) return "redirect:/admin/orders";
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
                                    RedirectAttributes ra,
                                    HttpSession session) {
        String redirect = checkAdminAccess(session);
        if (redirect != null) return redirect;

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

    @GetMapping("/userManagement")
    public String showUserManagementPage(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/userManagement";
    }

    @PostMapping("/userManagement/{id}/ban")
    public String banUser(@PathVariable int id) {
        userService.updateUserStatus(id, UserStatus.BANNED);
        return "redirect:/admin/userManagement";
    }

    @PostMapping("/userManagement/{id}/unban")
    public String unbanUser(@PathVariable int id) {
        userService.updateUserStatus(id, UserStatus.ACTIVE);
        return "redirect:/admin/userManagement";
    }

    @GetMapping("/userManagement/{id}")
    public String getUser(@PathVariable int id, Model model) {
        model.addAttribute("user", userService.findUserById(id));
        return "admin/userDetail";
    }
}
