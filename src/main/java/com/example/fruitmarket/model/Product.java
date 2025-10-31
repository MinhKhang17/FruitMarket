package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Table
@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String product_name;

    @Column
    private String product_description;

    @Column
    private double product_price;

    @Column
    private List<Image> images;

}
