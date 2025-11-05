package com.example.fruitmarket.dto;

import com.example.fruitmarket.enums.GhnStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class GhnOrderStatusPayload {
    @JsonProperty("ClientOrderCode")
    private long ClientOrderCode = 1; // mã đơn nội bộ
    @JsonProperty("OrderCode")
    private String OrderCode;       // mã đơn GHN
    @JsonProperty("Status")
    private GhnStatus Status;          // ví dụ: delivering, delivered, cancel...
    @JsonProperty("CODAmount")
    private Integer CODAmount;
}
