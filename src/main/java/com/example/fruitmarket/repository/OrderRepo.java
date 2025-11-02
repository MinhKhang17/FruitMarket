package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    List<Order> findAllByUsersOrderByIdDesc(Users users);
    Order findOrderById(Long id); // Có thể trả về null
    Optional<Order> findById(Long id); // Trả về Optional
    List<Order> findAll();
}