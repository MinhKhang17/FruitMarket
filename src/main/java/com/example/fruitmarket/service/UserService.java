package com.example.fruitmarket.service;

import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.User;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface UserService {
    User register(User user);
    String createVerificationToken(User user);
    boolean verifyToken(String token);
    User login(String username, String rawPassword);
    User findByUsername(String username);

    List<User_detail> getUserDetailFromSession(HttpSession session);
}
