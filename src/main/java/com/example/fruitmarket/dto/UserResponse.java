package com.example.fruitmarket.dto;

import com.example.fruitmarket.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private int id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private UserStatus status;

}
