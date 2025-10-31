package com.example.fruitmarket.service;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
}
