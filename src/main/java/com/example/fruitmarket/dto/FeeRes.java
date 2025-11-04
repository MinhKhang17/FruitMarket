package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class FeeRes {
    private int code;
    private String message;
    private FeeData data;

    @Data
    public static class FeeData {
        private Integer total;
        private Integer service_fee;
        private Integer insurance_fee;
        private Integer pick_station_fee;
        private Integer coupon_value;
    }
}