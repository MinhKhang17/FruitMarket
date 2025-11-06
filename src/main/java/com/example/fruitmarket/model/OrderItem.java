package com.example.fruitmarket.model;

import com.example.fruitmarket.enums.Units;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ProductVariant productVariant;

    @Column
    private Integer quantity; // chỉ dùng nếu unit != KILOGRAM

    @Column
    private Double weight; // chỉ dùng nếu unit == KILOGRAM

    @Enumerated(EnumType.STRING)
    private Units unit;

    @Column
    private BigDecimal price; // giá đơn vị tại thời điểm đặt

    public BigDecimal getSubTotal() {
        if (unit == Units.KILOGRAM) {
            return price.multiply(BigDecimal.valueOf(weight != null ? weight : 0));
        } else {
            return price.multiply(BigDecimal.valueOf(quantity != null ? quantity : 0));
        }
    }
}
