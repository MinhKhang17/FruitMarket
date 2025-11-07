package com.example.fruitmarket.model;

import com.example.fruitmarket.enums.ProductStatus;
import com.example.fruitmarket.enums.Units;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String productName;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String product_description;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Categorys category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brands brand;

    // One product -> many variants
    @OneToMany(mappedBy = "product", cascade = { CascadeType.MERGE, CascadeType.PERSIST }, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Units unit;
}
