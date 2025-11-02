package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.CheckoutProcessRequest;
import com.example.fruitmarket.dto.CheckoutRequest;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.Users;
import com.example.fruitmarket.service.OrderService;
import com.example.fruitmarket.service.ProductService;
import com.example.fruitmarket.service.UserService;
import com.example.fruitmarket.service.VnPayService;
import com.example.fruitmarket.util.QrUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class BuyController {
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private OrderService orderService;
    @Autowired
    private VnPayService vnPayService;

    @PostMapping("/checkout")
    public String checkout(@ModelAttribute CheckoutRequest checkoutRequest,
                           Model model,
                           HttpSession session,
                           RedirectAttributes ra) {

        // 1. Kiểm tra login trước

        if (session.getAttribute("loggedUser")==null) {
            ra.addFlashAttribute("message","You should login first");
            ra.addFlashAttribute("type","danger");
            return "redirect:/auth/login";
        }

        var productVariant = FruitMapper.toProductCheckout(productService.findProductVariantById(checkoutRequest.getProduct_variant_id()));
        model.addAttribute("productVariant", productVariant);
        model.addAttribute("quantity", checkoutRequest.getQuantity());

        session.setAttribute("productVariant", productVariant);
        session.setAttribute("quantity", checkoutRequest.getQuantity());
        List<User_detail> user = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail",user);

        return "home/checkout";
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@ModelAttribute CheckoutProcessRequest checkoutRequest,
                                  HttpSession session,
                                  RedirectAttributes ra,
                                  Model model,
                                  HttpServletRequest request) {
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

        var variant = productService.findProductVariantById(variantId);
        if (variant == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/";
        }

        if (quantity == null || quantity <= 0 || quantity > variant.getStock()) {
            ra.addFlashAttribute("message", "Số lượng không hợp lệ.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/product/" + variant.getProduct().getId();
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            ra.addFlashAttribute("message", "Vui lòng chọn phương thức thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }

        if (addressId == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn địa chỉ giao hàng.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }

        // 👉 Tạo Order trong DB
        Order order = orderService.createOrder(session, variant, quantity, addressId, paymentMethod);
        productService.decreaseStock(variantId, quantity);

        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            try {
                String orderInfo = "Thanh toan don hang #" + order.getId();
                String paymentUrl = vnPayService.createPaymentUrl(request, order.getTotalPrice(), orderInfo, order.getId());

                String qrBase64 = QrUtils.generateQrBase64(paymentUrl);

                model.addAttribute("paymentUrl", paymentUrl);
                model.addAttribute("qrBase64", qrBase64);
                model.addAttribute("order", order);
                model.addAttribute("amount", order.getTotalPrice());

                return "home/payment_qr";
            } catch (Exception e) {
                e.printStackTrace();
                ra.addFlashAttribute("message", "Lỗi tạo QR thanh toán VNPAY: " + e.getMessage());
                ra.addFlashAttribute("type", "danger");
                return "redirect:/";
            }
        }

        ra.addFlashAttribute("message", "Đặt hàng thành công!");
        return "redirect:/order/success";
    }

    @GetMapping("/order/success")
    public String orderSuccess(RedirectAttributes ra,HttpSession session) {
        ra.addFlashAttribute("message","buy success");
        ra.addFlashAttribute("type","success");
        return "redirect:/";
    }

    @PostMapping("/checkout/save-address")
    public String saveAddress(@RequestParam String phone,
                              @RequestParam String address,
                              HttpSession session,
                              RedirectAttributes ra) {
        Users loggedUser = (Users) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            ra.addFlashAttribute("message", "Please login first");
            return "redirect:/auth/login";
        }

        User_detail detail = new User_detail();
        detail.setPhone(phone);
        detail.setAddress(address);
        detail.setUser(loggedUser);

        userService.saveUserDetail(detail);

        ra.addFlashAttribute("message", "Đã thêm địa chỉ giao hàng mới!");
        ra.addFlashAttribute("type", "success");

        // Quay lại checkout để hiển thị thông tin mới
        return "redirect:/checkout";
    }
    @GetMapping("/checkout")
    public String getCheckoutPage(HttpSession session, Model model) {
        Users loggedUser = (Users) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/auth/login";

        List<User_detail> userDetails = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail", userDetails);

        // nếu bạn có lưu productVariant, quantity trong session thì lấy lại
        Object productVariant = session.getAttribute("productVariant");
        Object quantity = session.getAttribute("quantity");
        model.addAttribute("productVariant", productVariant);
        model.addAttribute("quantity", quantity);

        return "home/checkout";
    }
}
