package com.example.fruitmarket.model;

import com.example.fruitmarket.enums.GhnStatus;
import com.example.fruitmarket.enums.OrderStauts;
import com.example.fruitmarket.enums.PricingMethod;
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
    @Column(columnDefinition = "NVARCHAR(255)")
    private String address;
    @Column
    private String phoneNumber;

    @Column
    private BigDecimal totalPrice;

    @Column
    private String ghnOrderCode;

    @Enumerated(EnumType.STRING)
    private GhnStatus ghnStatus = GhnStatus.READY_TO_PICK;
    @Column
    private BigDecimal shippingFee;
    @Column(columnDefinition = "NVARCHAR(100)")
    private String recipientName;

    // SỬA TỪ @OneToOne THÀNH @OneToMany
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @Column
    private long totalQuantity;

    @Column
    private double totalWeight;

    @Column
    private String bankName;

    @Column
    private String bankReferenceCode;

    // Helper method để thêm payment
    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setOrder(this);
    }

    // Helper method để lấy payment gốc (PAY)
    public Payment getOriginalPayment() {
        return payments.stream()
                .filter(p -> "PAY".equals(p.getType()))
                .findFirst()
                .orElse(null);
    }

    // Helper method để lấy payment refund (REFUND)
    public Payment getRefundPayment() {
        return payments.stream()
                .filter(p -> "REFUND".equals(p.getType()))
                .findFirst()
                .orElse(null);
    }
}