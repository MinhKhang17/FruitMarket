package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class CalculateFeeRes {
    private Integer code;
    private String message;
    private DataObj data;

    @Data
    public static class DataObj {
        private Integer total;
    }
}