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
        model.addAttribute("userDetail",user);

        return "home/checkout";
    }

}
