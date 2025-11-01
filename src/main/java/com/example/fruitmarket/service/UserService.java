package com.example.fruitmarket.service;

import com.example.fruitmarket.model.User;

public interface UserService {
    User register(User user);
    String createVerificationToken(User user);
    boolean verifyToken(String token);
    User login(String username, String rawPassword);
    User findByUsername(String username);
}
