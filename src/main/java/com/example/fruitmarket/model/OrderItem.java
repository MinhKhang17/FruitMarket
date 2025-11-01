package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;

@Table
@Data
@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private long quanity;

    @ManyToOne
    private ProductVariant productVariant;

}
