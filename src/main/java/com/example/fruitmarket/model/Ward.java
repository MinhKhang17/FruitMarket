package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ward {
    @Id
    @Column(length = 32)
    private String wardCode;            // GHN ward_code là chuỗi

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String wardName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "district_id")
    private District district;
}