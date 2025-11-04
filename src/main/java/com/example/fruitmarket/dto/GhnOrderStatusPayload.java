package com.example.fruitmarket.dto;

import com.example.fruitmarket.enums.GhnStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class GhnOrderStatusPayload {
    private long ClientOrderCode = 1; // mã đơn nội bộ
    private String OrderCode;       // mã đơn GHN
    private GhnStatus Status;          // ví dụ: delivering, delivered, cancel...
    private Integer CODAmount;
}
