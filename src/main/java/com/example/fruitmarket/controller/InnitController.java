package com.example.fruitmarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InnitController {
    @GetMapping("/init")
    private String toMainPage(){
        return "redirect:/";
    }
}
