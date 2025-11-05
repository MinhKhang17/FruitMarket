package com.example.fruitmarket.util;

import com.example.fruitmarket.model.Users;
import jakarta.servlet.http.HttpSession;

public class AuthUtils {

    public static boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute("loggedUser") != null;
    }

    public static boolean isAdmin(HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public static boolean isClient(HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        return user != null
                && "CLIENT".equalsIgnoreCase(user.getRole().toString())
                && "ACTIVE".equalsIgnoreCase(user.getStatus().toString());
    }
}
