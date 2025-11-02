package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepo extends JpaRepository<Order,Long> {
    Order findOrderById(Long Id);
    List<Order> findAllByUsersOrderByIdDesc(Users user);

}
