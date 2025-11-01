package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Users;

public interface UserService {
    Users register(Users user);
    String createVerificationToken(Users user);
    boolean verifyToken(String token);
    Users login(String username, String rawPassword);
    Users findByUsername(String username);
}
