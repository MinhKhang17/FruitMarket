package com.example.fruitmarket.controller;

import com.example.fruitmarket.Dto.CheckoutRequest;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.Users;
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

import java.util.List;

@Controller
public class BuyController {
    @Autowired private ProductService productService;
    @Autowired private UserService userService;

    @PostMapping("/checkout")
    public String checkout(@ModelAttribute CheckoutRequest checkoutRequest, Model model, HttpSession session, RedirectAttributes ra) {
        model.addAttribute("productVariant", FruitMapper.toProductCheckout(productService.findProductVariantById(checkoutRequest.getProduct_variant_id())));
        model.addAttribute("quantity", checkoutRequest.getQuantity());

        List<User_detail> user =  userService.getUserDetailFromSession(session);
        if(session.getAttribute("loggedUser") == null) {
            ra.addFlashAttribute("message","You should login first");
            ra.addFlashAttribute("type","danger");
            return "redirect:/auth/login";
        }

        var productVariant = FruitMapper.toProductCheckout(productService.findProductVariantById(checkoutRequest.getProduct_variant_id()));
        model.addAttribute("productVariant", productVariant);
        model.addAttribute("quantity", checkoutRequest.getQuantity());

        session.setAttribute("productVariant", productVariant);
        session.setAttribute("quantity", checkoutRequest.getQuantity());

        model.addAttribute("userDetail",user);

        return "home/checkout";
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
