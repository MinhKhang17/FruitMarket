package com.example.fruitmarket.model;

import com.example.fruitmarket.enums.ImageType;
import jakarta.persistence.*;
import lombok.Data;

// Image.java
@Entity
@Table(name = "images")
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
