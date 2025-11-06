package com.example.fruitmarket.enums;

public enum OrderStauts {
    PENDING,    // 1
    SHIPPING,   // 2
    SHIPPED,    // 3
    COMPLETED,  // 4
    CANCELLED;   // 0 (đặc biệt)

    public int rank() {
        return switch (this) {
            case CANCELLED -> 0;
            case PENDING  -> 1;
            case SHIPPING -> 2;
            case SHIPPED  -> 3;
            case COMPLETED -> 4;
        };
    }
}
