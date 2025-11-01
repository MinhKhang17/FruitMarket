package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDetailRepo extends JpaRepository<User_detail,Integer> {
    List<User_detail> findAllByUser(Users users);
}
