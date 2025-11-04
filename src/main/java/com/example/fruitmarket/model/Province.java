package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Province {
    @Id
    private Integer provinceId;
    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String provinceName;
}
