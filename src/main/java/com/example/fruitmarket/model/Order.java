package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Table
@Entity
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Users user;

    @OneToMany
    private List<OrderItem> orderItems  ;

    @Column
    private boolean isPaid = false;

    @Column
    private double price;

    @Column
    private double total;
}
