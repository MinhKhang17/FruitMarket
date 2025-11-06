package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class GhnCancelResp {
    private Integer code;       // 200 nếu OK
    private Boolean success;    // đôi khi có
    private String message;     // thông báo
    private String error;       // lỗi nếu có
    private Object data;        // data (tuỳ GHN)
}