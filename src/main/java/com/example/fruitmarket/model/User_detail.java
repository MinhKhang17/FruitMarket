package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;
import org.apache.catalina.User;

@Table
@Entity
@Data
public class User_detail {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column
    private String phone;
   @Column
    private String address;

   @ManyToOne
    private Users user;

}
