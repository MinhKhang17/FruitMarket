package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class District {
    @Id
    private Integer districtId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String districtName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "province_id")
    private Province province;
}