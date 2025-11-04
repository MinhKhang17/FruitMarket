package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class FeeQuoteReq {
    private Integer toDistrictId;
    private String toWardCode;
    private Integer serviceTypeId; // optional nếu dùng service_id
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer insuranceValue;
}