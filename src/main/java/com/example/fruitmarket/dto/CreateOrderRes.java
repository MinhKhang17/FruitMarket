package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class CreateOrderRes {
    private int code;
    private String message;
    private DataObj data;

    @lombok.Data
    public static class DataObj {
        private String orderCode;    // GHN trả "order_code"
        private String expected_delivery_time;
    }
}