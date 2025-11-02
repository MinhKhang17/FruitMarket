// file: src/main/java/com/example/fruitmarket/controller/UserProfileController.java
package com.example.fruitmarket.controller;

import com.example.fruitmarket.Dto.ChangePasswordRequest;
import com.example.fruitmarket.Dto.ProfileUpdateRequest;
import com.example.fruitmarket.model.Users;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/client")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Users logged = (Users) session.getAttribute("loggedUser");
        if (logged == null) return "redirect:/auth/login";

        Users user = userService.findByUsername(logged.getUsername());
        List<User_detail> addresses = userService.getUserDetailFromSession(session);

        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        model.addAttribute("profileForm", new ProfileUpdateRequest(user.getEmail(), user.getPhone()));
        model.addAttribute("pwdForm", new ChangePasswordRequest());

        return "client/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateRequest form,
                                BindingResult rs,
                                HttpSession session,
                                Model model) {
        Users logged = (Users) session.getAttribute("loggedUser");
        if (logged == null) return "redirect:/auth/login";

        if (rs.hasErrors()) {
            model.addAttribute("user", logged);
            model.addAttribute("addresses", userService.getUserDetailFromSession(session));
            model.addAttribute("pwdForm", new ChangePasswordRequest());
            return "client/profile";
        }

        try {
            Users updated = userService.updateProfile(logged.getId(), form.getEmail(), form.getPhone());
            session.setAttribute("loggedUser", updated);
            model.addAttribute("user", updated);
            model.addAttribute("success", "Cập nhật hồ sơ thành công!");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("user", logged);
        }

        model.addAttribute("addresses", userService.getUserDetailFromSession(session));
        model.addAttribute("pwdForm", new ChangePasswordRequest());
        return "client/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("pwdForm") ChangePasswordRequest form,
                                 BindingResult rs,
                                 HttpSession session,
                                 Model model) {
        Users logged = (Users) session.getAttribute("loggedUser");
        if (logged == null) return "redirect:/auth/login";

        if (rs.hasErrors()) {
            model.addAttribute("user", logged);
            model.addAttribute("addresses", userService.getUserDetailFromSession(session));
            model.addAttribute("profileForm", new ProfileUpdateRequest(logged.getEmail(), logged.getPhone()));
            return "client/profile";
        }

        try {
            userService.changePassword(logged.getId(), form.getOldPassword(), form.getNewPassword());
            model.addAttribute("successPwd", "Đổi mật khẩu thành công!");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorPwd", ex.getMessage());
        }

        Users fresh = userService.findByUsername(logged.getUsername());
        session.setAttribute("loggedUser", fresh);
        model.addAttribute("user", fresh);
        model.addAttribute("addresses", userService.getUserDetailFromSession(session));
        model.addAttribute("profileForm", new ProfileUpdateRequest(fresh.getEmail(), fresh.getPhone()));
        return "client/profile";
    }
}
