// src/main/java/com/example/fruitmarket/service/GhnClientServiceImpl.java
package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GhnClientServiceImpl implements GhnClientService {

    private final WebClient ghnClient;
    private final String shopId;

    public GhnClientServiceImpl(WebClient ghnClient,
                                @Value("${ghn.shop-id}") String shopId) {
        this.ghnClient = ghnClient;
        this.shopId = shopId;
    }

    @Override
    public AvailableServicesRes availableServices(int fromDistrictId, int toDistrictId) {
        Map<String, Object> body = new HashMap<>();
        body.put("shop_id", Integer.valueOf(shopId));            // ← giờ mới có biến shopId
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);

        return ghnClient.post()
                .uri("/v2/shipping-order/available-services")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AvailableServicesRes.class)
                .block();
    }

    @Override
    public FeeRes calculateFee(int fromDistrictId, int toDistrictId, String toWardCode,
                               int serviceId, int weight, int length, int width, int height,
                               int insuranceValue) {
        Map<String, Object> body = new HashMap<>();
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("service_id", serviceId); // dùng service_id (ổn định hơn service_type_id)
        body.put("weight", weight);
        body.put("length", length);
        body.put("width", width);
        body.put("height", height);
        body.put("insurance_value", Math.max(0, insuranceValue));

        return ghnClient.post()
                .uri("/v2/shipping-order/fee")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(FeeRes.class)
                .block();
    }

    @Override
    public CreateOrderRes createOrder(CreateOrderReq req) {
        Map<String, Object> body = new HashMap<>();
        body.put("to_name", req.getToName());
        body.put("to_phone", req.getToPhone());
        body.put("to_address", req.getToAddress());
        body.put("to_ward_code", req.getToWardCode());
        body.put("to_district_id", req.getToDistrictId());
        body.put("service_id", req.getServiceId());
        body.put("weight", req.getWeight());
        body.put("length", req.getLength());
        body.put("width", req.getWidth());
        body.put("height", req.getHeight());
        body.put("payment_type_id", req.getPayment_type_id() == null ? 1 : req.getPayment_type_id());
        body.put("required_note", req.getRequired_note() == null ? "KHONGCHOXEMHANG" : req.getRequired_note());
        body.put("client_order_code", req.getClientOrderCode());
        body.put("note", req.getNote());
        body.put("cod_amount", req.getCodAmount() == null ? 0 : req.getCodAmount());
        body.put("items", req.getItems());

        return ghnClient.post()
                .uri("/v2/shipping-order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CreateOrderRes.class)
                .block();
    }

    @Override
    public OrderDetailRes getOrderDetail(String orderCode) {
        Map<String, Object> body = Map.of("order_code", orderCode);
        return ghnClient.post()
                .uri("/v2/shipping-order/detail")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OrderDetailRes.class)
                .block();
    }
}
