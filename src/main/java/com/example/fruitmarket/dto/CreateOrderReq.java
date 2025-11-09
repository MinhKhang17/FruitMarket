package com.example.fruitmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrderReq {
    @JsonProperty("to_name")
    private String toName;
    @JsonProperty("to_phone")
    private String toPhone;
    @JsonProperty("to_address")
    private String toAddress;
    @JsonProperty("to_ward_code")
    private String toWardCode;     // bắt buộc
    @JsonProperty("to_district_id")
    private Integer toDistrictId;  // bắt buộc
    @JsonProperty("from_district_id")
    private Integer fromDistrictId; // bắt buộc
    @JsonProperty("from_ward_code")
    private String fromWardCode;

    @JsonProperty("service_id")
    private Integer serviceId;     // dùng service_id

    @JsonProperty("weight")
    private Long weight;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("cod_amount")
    private Integer codAmount;
    @JsonProperty("payment_type_id")
    private Integer payment_type_id; // 1 shop trả, 2 KH trả
    @JsonProperty("required_note")
    private String required_note;    // KHONGCHOXEMHANG...
    @JsonProperty("client_order_code")
    private String clientOrderCode;
    @JsonProperty("note")
    private String note;

    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private long quantity;
    }
}