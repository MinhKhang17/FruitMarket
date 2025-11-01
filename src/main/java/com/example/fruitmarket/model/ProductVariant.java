package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Variant thuộc product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // Ví dụ: "1kg", "500g", "Red / Large" (tuỳ model)
    @Column
    private String name;




    // Dùng BigDecimal cho money
    @Column(precision = 15, scale = 2)
    private BigDecimal price;
}
