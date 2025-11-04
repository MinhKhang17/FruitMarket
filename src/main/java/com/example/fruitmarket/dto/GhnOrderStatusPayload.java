package com.example.fruitmarket.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class GhnOrderStatusPayload {
    private String ClientOrderCode; // mã đơn nội bộ
    private String OrderCode;       // mã đơn GHN
    private String Status;          // ví dụ: delivering, delivered, cancel...
    private Integer CODAmount;
}
