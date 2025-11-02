package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Payment;
import com.example.fruitmarket.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public void createPayment(Payment payment) {
        paymentRepository.save(payment);
    }
}
