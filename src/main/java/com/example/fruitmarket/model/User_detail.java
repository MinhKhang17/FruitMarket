package com.example.fruitmarket.model;

import jakarta.persistence.*;
import lombok.Data;
import org.apache.catalina.User;

@Table
@Entity
@Data
public class User_detail {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column
    private String phone;
   @Column(columnDefinition = "NVARCHAR(255)")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;  // ✅ FK -> province.id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;  // ✅ FK -> district.id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_code")
    private Ward ward;          // ✅ FK -> ward.code

    @Column(columnDefinition = "NVARCHAR(100)")
    private String receiverName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;
}
