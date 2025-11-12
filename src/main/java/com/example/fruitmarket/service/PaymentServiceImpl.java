package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Payment;
import com.example.fruitmarket.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public void createPayment(Payment payment) {
        paymentRepository.save(payment);
    }

    @Override
    public Optional<Payment> getPaymentByOrderIdAndTypePay(Long orderId, String type) {
        return paymentRepository.findByOrderIdAndType(orderId, type);
    }

    @Override
    public boolean isTransactionIdExist(String id) {
        return paymentRepository.findByTransactionIdAndType(id, "REFUND")==null;
    }
}
