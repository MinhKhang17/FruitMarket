package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.UserResponse;
import com.example.fruitmarket.enums.UserStatus;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.model.Users;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface UserService {
    Users register(Users user);
    String createVerificationToken(Users user);
    boolean verifyToken(String token);
    Users login(String username, String rawPassword);
    Users findByUsername(String username);

    List<User_detail> getUserDetailFromSession(HttpSession session);

    User_detail findUserDetalById(Long addressId);
    User_detail saveUserDetail(User_detail userDetail);

    Users updateProfile(int userId, String email, String phone);
    void changePassword(int userId, String oldPassword, String newPassword);

    Users updateUserStatus(int id, UserStatus status);
    List<UserResponse> getAllUsers();
    UserResponse findUserById(int id);

    void updateAddress(Long addressId,
                       Integer provinceId,
                       Integer districtId,
                       String wardCode,
                       String address,
                       String phone);
}
