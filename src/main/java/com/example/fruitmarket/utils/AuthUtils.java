package com.example.fruitmarket.utils;

import com.example.fruitmarket.model.User;
import jakarta.servlet.http.HttpSession;

public class AuthUtils {
    public static boolean isLoggedIn(HttpSession session){
        if (session.getAttribute("loggedUser") != null) return false;
        return true;
    }

    public static boolean isAdmin(HttpSession session){
        User user = (User) session.getAttribute("loggedUser");
        if (user!=null && user.getRole().equals("ADMIN")) return true;
        return false;
    }

    public static boolean isCustomer(HttpSession session){
        User user = (User) session.getAttribute("loggedUser");
        if (user!=null && user.getRole().equals("CUSTOMER")) return true;
        return false;
    }
}
