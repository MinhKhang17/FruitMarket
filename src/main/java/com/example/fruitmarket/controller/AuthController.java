package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Users;
import com.example.fruitmarket.service.UserService;
import com.example.fruitmarket.util.AuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginForm(Model model,HttpSession session) {
        if(com.example.fruitmarket.util.UserUtil.isLogin(session)){return "redirect:/";}
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new Users());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@ModelAttribute("user") Users form,
                          RedirectAttributes ra,
                          HttpSession session) {

        if (form.getUsername() == null || form.getUsername().isBlank()
                || form.getPassword() == null || form.getPassword().isBlank()) {
            ra.addFlashAttribute("error", "Tên đăng nhập và mật khẩu không được để trống.");
            ra.addFlashAttribute("user", form);
            return "redirect:/auth/login";
        }

        Users user = userService.login(form.getUsername(), form.getPassword());
        if (user == null) {
            ra.addFlashAttribute("error", "Đăng nhập thất bại — kiểm tra tài khoản/mật khẩu hoặc xác thực email.");
            ra.addFlashAttribute("user", form);
            return "redirect:/auth/login";
        }

        session.setAttribute("loggedUser", user);

        ra.addFlashAttribute("success", "Đăng nhập thành công. Chào " + user.getUsername() + "!");
        if (AuthUtils.isAdmin(session)) return "redirect:/admin/adminPage";
        return "redirect:/";
    }


    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new Users());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("user") @Valid Users form,
                             BindingResult result,
                             RedirectAttributes ra) {

        if (result.hasErrors()) {

            if (result.hasFieldErrors("phone")) {
                form.setPhone("");
                result.rejectValue("phone", null, "");
            }

            ra.addFlashAttribute("org.springframework.validation.BindingResult.user", result);
            ra.addFlashAttribute("user", form);
            ra.addFlashAttribute("error", "Dữ liệu đăng ký không hợp lệ, vui lòng kiểm tra lại.");
            return "redirect:/auth/register";
        }

        try {
            userService.register(form);
            ra.addFlashAttribute("success", "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            String message = e.getMessage();
            if (message.contains("Tên đăng nhập")) form.setUsername("");
            if (message.contains("Email")) form.setEmail("");
            if (message.contains("Số điện thoại")) form.setPhone("");

            ra.addFlashAttribute("error", "Đăng ký thất bại: " + message);
            ra.addFlashAttribute("user", form);
            return "redirect:/auth/register";
        }
    }


    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes ra) {
        boolean ok = userService.verifyToken(token);
        if (ok) {
            ra.addFlashAttribute("success", "Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.");
        } else {
            ra.addFlashAttribute("error", "Xác thực thất bại hoặc token đã hết hạn.");
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("success", "Bạn đã đăng xuất.");
        return "redirect:/auth/login";
    }
}
