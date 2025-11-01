package com.example.fruitmarket.controller;

import com.example.fruitmarket.Dto.CheckoutProcessRequest;
import com.example.fruitmarket.Dto.CheckoutRequest;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.Users;
import com.example.fruitmarket.service.OrderService;
import com.example.fruitmarket.service.ProductService;
import com.example.fruitmarket.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BuyController {
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private OrderService orderService;
    @PostMapping("/checkout")
    public String checkout(@ModelAttribute CheckoutRequest checkoutRequest,
                           Model model,
                           HttpSession session,
                           RedirectAttributes ra) {

        // 1. Kiểm tra login trước
        Object logged = session.getAttribute("loggedUser");
        if (logged == null) {
            ra.addFlashAttribute("message","You should login first");
            ra.addFlashAttribute("type","danger");
            return "redirect:/auth/login";
        }

        // 2. Lấy productVariant an toàn (không null)
        var variant = productService.findProductVariantById(checkoutRequest.getProduct_variant_id());
        model.addAttribute("productVariant", FruitMapper.toProductCheckout(variant));
        model.addAttribute("quantity", checkoutRequest.getQuantity());

        // 3. Lấy danh sách địa chỉ của user (trả List<User_detail>, có thể rỗng)
        List<User_detail> userDetails = userService.getUserDetailFromSession(session); // đảm bảo trả List
        if (userDetails == null) userDetails = Collections.emptyList();
        model.addAttribute("userDetail", userDetails);

        // 4. Trả view
        return "home/checkout";
    }
    @PostMapping("/checkout/process")
    public String processCheckout(@ModelAttribute CheckoutProcessRequest checkoutRequest,
                                  HttpSession session,
                                  RedirectAttributes ra,
                                  Model model) {
        // Kiểm tra đăng nhập
        if (session.getAttribute("loggedUser") == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập trước khi thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        // Lấy thông tin từ request
        Long variantId = checkoutRequest.getVariantId();
        Integer quantity = checkoutRequest.getQuantity();
        Long addressId = checkoutRequest.getAddressId();
        String paymentMethod = checkoutRequest.getPaymentMethod();

        // Lấy variant từ DB
        var variant = productService.findProductVariantById(variantId);
        if (variant == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/";
        }

        // Kiểm tra số lượng hợp lệ
        if (quantity == null || quantity <= 0 || quantity > variant.getStock()) {
            ra.addFlashAttribute("message", "Số lượng không hợp lệ.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/product/" + variant.getProduct().getId();
        }

        // ✅ Kiểm tra phương thức thanh toán
        if (paymentMethod == null || paymentMethod.isBlank()) {
            ra.addFlashAttribute("message", "Vui lòng chọn phương thức thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }

        // ✅ Kiểm tra địa chỉ giao hàng
        if (addressId == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn địa chỉ giao hàng.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }

        // 👉 Tại đây bạn có thể tạo Order / OrderDetail
        Order order = orderService.createOrder(session, variant, quantity, addressId, paymentMethod);

        // Giảm stock
        productService.decreaseStock(variantId, quantity);

        // Redirect sang trang xác nhận hoặc cảm ơn
        ra.addFlashAttribute("message", "Đặt hàng thành công!");
        return "redirect:/order/success";
    }
    @GetMapping("/order/success")
    public String orderSuccess(RedirectAttributes ra,HttpSession session) {
        ra.addFlashAttribute("message","buy success");
        ra.addFlashAttribute("type","success");
        return "redirect:/";
    }



}
