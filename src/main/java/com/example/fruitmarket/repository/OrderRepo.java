package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepo extends JpaRepository<Order,Integer> {
    Order findById(Long Id);
}
