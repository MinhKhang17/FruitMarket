package com.example.fruitmarket.util;

import com.example.fruitmarket.model.Users;
import jakarta.servlet.http.HttpSession;

public class UserUtil {
    public static boolean isLogin(HttpSession session) {
        if (session.getAttribute("loggedUser") == null) return false;
        else return true;
    };
    public static Users getUserFromSession(HttpSession session) {
        return (Users) session.getAttribute("loggedUser");

    }
    public static boolean isAdmin(HttpSession session) {
        if(getUserFromSession(session).getRole().equals("admin")) return true;
        else return false;
    }
}
