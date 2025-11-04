package com.example.fruitmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class AvailableServicesRes {
    private int code;
    private String message;
    private List<ServiceItem> data;

    @Data
    public static class ServiceItem {
        @JsonProperty("service_id")
        private Integer serviceId;
        private Integer service_type_id;
        private String short_name;
        private String service_code;
    }
}