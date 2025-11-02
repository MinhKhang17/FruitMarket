package com.example.fruitmarket.model;

import com.example.fruitmarket.Enums.OrderStauts;
import com.example.fruitmarket.Enums.PricingMethod;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Table(name = "orders")
@Entity
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> orderItemList = new ArrayList<>();

    @ManyToOne
    private Users users;

    @Column
    private boolean isPaid;

    @Enumerated(EnumType.STRING)
    private OrderStauts orderStauts;

    @Enumerated(EnumType.STRING)
    private PricingMethod pricingMethod;
    @Column
    private String address;
    @Column
    private String phoneNumber;
    @Column
    private BigDecimal totalPrice;
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;


    @Column
    private int totalQuantity;

}
