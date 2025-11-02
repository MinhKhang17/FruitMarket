package com.example.fruitmarket.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

    public static LocalDateTime parsePayDate(String payDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return LocalDateTime.parse(payDate, formatter);
    }
}
