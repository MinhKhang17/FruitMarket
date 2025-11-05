package com.example.fruitmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FeeRes {
    @JsonProperty("code")
    private int code;
    @JsonProperty("message")
    private String message;
    @JsonProperty("data")
    private FeeData data;

    @Data
    public static class FeeData {
        @JsonProperty("total")
        private Integer total;
        @JsonProperty("service_fee")
        private Integer service_fee;
        @JsonProperty("insurance_fee")
        private Integer insurance_fee;
        @JsonProperty("pick_station_fee")
        private Integer pick_station_fee;
        @JsonProperty("coupon_value")
        private Integer coupon_value;
    }
}