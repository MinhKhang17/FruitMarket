package com.example.fruitmarket.model;

import com.example.fruitmarket.Dtos.ImageType;
import jakarta.persistence.*;
import lombok.Data;

@Table
@Entity
@Data
public class Image {
@Id
@GeneratedValue(strategy= GenerationType.IDENTITY)
    private String id;

@Column(unique = true)
    private String url;

@Column
    private ImageType imageType;


}
