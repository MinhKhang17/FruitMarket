package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class OrderDetailRes {
    private int code;
    private String message;
    private DataObj data;

    @lombok.Data
    public static class DataObj {
        private String status;
        private String updated_date;
        private String order_code;
        private String expected_delivery_time;
    }
}