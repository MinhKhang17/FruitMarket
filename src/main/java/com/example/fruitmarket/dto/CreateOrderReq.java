package com.example.fruitmarket.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderReq {
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardCode;     // bắt buộc
    private Integer toDistrictId;  // bắt buộc

    private Integer serviceId;     // dùng service_id
    private long weight;        // gram
    private Integer length;        // cm
    private Integer width;         // cm
    private Integer height;        // cm

    private Integer codAmount;
    private Integer payment_type_id; // 1 shop trả, 2 KH trả
    private String required_note;    // KHONGCHOXEMHANG...
    private String clientOrderCode;
    private String note;

    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private long quantity;
    }
}