package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class RefundRequest {
    private Long orderId;
    private String referenceCode;
    private String transactionDate;
    private String transactionTime;
}