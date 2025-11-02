package com.example.fruitmarket.model;

import com.example.fruitmarket.Enums.ImageType;
import jakarta.persistence.*;
import lombok.Data;

// Image.java
@Entity
@Table(name = "images")   // <-- chọn "images" và dùng nhất quán
@Data
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    private ImageType imageType;
}
