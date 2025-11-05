package com.example.fruitmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateOrderRes {
    @JsonProperty("code")
    private String code;
    @JsonProperty("message")
    private String message;
    @JsonProperty("data")
    private DataObj data;

    @lombok.Data
    public static class DataObj {
        @JsonProperty("order_code")
        private String orderCode;    // GHN trả "order_code"
        @JsonProperty("expected_delivery_time")
        private String expectedDeliveryTime;
    }
}