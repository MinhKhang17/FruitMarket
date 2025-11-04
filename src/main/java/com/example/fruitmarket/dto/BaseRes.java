package com.example.fruitmarket.dto;

import lombok.Data;

@Data
public class BaseRes<T> {
    private int code;
    private String message;
    private T data;
}
