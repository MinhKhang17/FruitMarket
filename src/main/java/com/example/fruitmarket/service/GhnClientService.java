package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface GhnClientService {
    AvailableServicesRes availableServices(int fromDistrictId, int toDistrictId);
    FeeRes calculateFee(int fromDistrictId, int toDistrictId, String toWardCode,
                        int serviceId, long weight, int length, int width, int height,
                        int insuranceValue);
    CreateOrderRes createOrder(CreateOrderReq req);
    OrderDetailRes getOrderDetail(String orderCode);

    Optional<String> createOrderAndGetOrderCode(CreateOrderReq req);
    void cancelOrder(String orderCode);
}
