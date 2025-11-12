package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByOrderIdAndType(Long orderId, String type);
    Payment findByTransactionIdAndType(String transactionNo, String type);
}
