package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDetailRepo extends JpaRepository<User_detail,Integer> {
    List<User_detail> findAllByUser(Users users);

    Optional<User_detail> findById(Long addressId);
}
