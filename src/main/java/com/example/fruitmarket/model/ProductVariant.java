package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Table
@Entity
@Data
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToMany
    private List<Product> products;
    @OneToOne(cascade = CascadeType.ALL)
    private Image image;

    @Column
    private double product_price;
}
