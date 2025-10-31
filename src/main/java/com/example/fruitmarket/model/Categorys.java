package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
@Data
@RequiredArgsConstructor
public class Categorys {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;



    // 1 Category -> n Product
//    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = false)
//    private List<Product> products = new ArrayList<>();
}
