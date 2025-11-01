package com.example.fruitmarket.model;


import jakarta.persistence.*;
import lombok.Data;

@Table
@Entity
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private ProductVariant productVariant;

    @ManyToOne
    private Order order;

    @Column
    private long quanity;

    @Column
    private double price;
}
