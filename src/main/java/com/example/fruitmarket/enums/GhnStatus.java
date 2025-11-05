package com.example.fruitmarket.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GhnStatus {
    READY_TO_PICK,
    PICKING,
    PICKED,
    STORING,
    TRANSPORTING,
    DELIVERING,
    DELIVERED,
    RETURN,
    RETURNED,
    DELAY,
    CANCEL,
    LOST,
    EXCEPTION;

    @JsonCreator
    public static GhnStatus fromString(String value) {
        return GhnStatus.valueOf(value.toUpperCase());
    }
}
