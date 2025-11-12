package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Payment;

import java.util.Optional;

public interface PaymentService {
    void createPayment(Payment payment);
    Optional<Payment> getPaymentByOrderIdAndTypePay(Long orderId, String type);
    boolean isTransactionIdExist(String id);
}
