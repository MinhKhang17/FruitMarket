package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;

@Table(name = "user_information")
@Entity
@Data
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String userName;

    @Column(unique = true)
    private String email;


    private String password;

    private Boolean isValidEmail =false;
}
