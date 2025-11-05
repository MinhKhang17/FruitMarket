package com.example.fruitmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderDetailRes {
    @JsonProperty("code")
    private int code;
    @JsonProperty("message")
    private String message;
    @JsonProperty("data")
    private DataObj data;

    @lombok.Data
    public static class DataObj {
        @JsonProperty("status")
        private String status;
        @JsonProperty("created_date")
        private String updated_date;
        @JsonProperty("order_code")
        private String orderCode;
        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;
    }
}