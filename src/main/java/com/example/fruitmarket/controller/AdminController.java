package com.example.fruitmarket.controller;

import ch.qos.logback.core.model.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping("/admin/adminPage")
    public  String adminPage(Model model){
        return "admin/adminPage";
    }
}
