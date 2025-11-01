package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;

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


}
