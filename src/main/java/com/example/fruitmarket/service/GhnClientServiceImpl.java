package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * GhnClientServiceImpl - phiên bản đã chỉnh:
 * - ObjectMapper không escape non-ASCII
 * - serialize JSON trước khi gửi (bodyValue(jsonString))
 * - thêm debugString để kiểm tra codepoints / UTF-8 bytes nếu cần
 * - build items fallback nếu req.getItems() == null (tránh GHN trả 400 "Tên hàng hóa bắt buộc")
 */
@Service
@Slf4j
public class GhnClientServiceImpl implements GhnClientService {

    private final WebClient ghnClient;
    private final String shopId;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

    public GhnClientServiceImpl(WebClient ghnClient,
                                @Value("${ghn.shop-id}") String shopId) {
        this.ghnClient = ghnClient;
        this.shopId = shopId;
    }

    @Override
    public AvailableServicesRes availableServices(int fromDistrictId, int toDistrictId) {
        Map<String, Object> body = new HashMap<>();
        try {
            body.put("shop_id", Integer.parseInt(shopId));
        } catch (NumberFormatException ex) {
            log.warn("Invalid ghn.shop-id value: {}", shopId);
            body.put("shop_id", shopId);
        }
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return ghnClient.post()
                    .uri("/v2/shipping-order/available-services")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(AvailableServicesRes.class)
                    .block();
        } catch (Exception e) {
            log.error("[GHN] availableServices error, body={}", safeToJson(body), e);
            return null;
        }
    }

    @Override
    public FeeRes calculateFee(int fromDistrictId, int toDistrictId, String toWardCode,
                               int serviceId, long weight, int length, int width, int height,
                               int insuranceValue) {
        Map<String, Object> body = new HashMap<>();
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);
        body.put("to_ward_code", toWardCode);
        body.put("service_id", serviceId);
        body.put("weight", weight);
        body.put("length", length);
        body.put("width", width);
        body.put("height", height);
        body.put("insurance_value", Math.max(0, insuranceValue));

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return ghnClient.post()
                    .uri("/v2/shipping-order/fee")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(FeeRes.class)
                    .block();
        } catch (Exception e) {
            log.error("[GHN] calculateFee error, body={}", safeToJson(body), e);
            return null;
        }
    }

    /**
     * Gọi GHN create order (an toàn, không throw WebClientResponseException khi 4xx)
     */
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

        // Build items: nếu caller không cung cấp items -> tạo fallback item để tránh lỗi GHN (Tên hàng hóa bắt buộc)
        Object incomingItems = req.getItems();
        if (incomingItems == null) {
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            // Có thể điều chỉnh "name" từ dữ liệu khác nếu bạn có (ví dụ req.getItemName())
            item.put("name", "Hàng hóa");
            item.put("quantity", 1);
            item.put("price", req.getCodAmount() == null ? 0 : req.getCodAmount());
            item.put("weight", req.getWeight() == null ? 0 : req.getWeight());
            items.add(item);
            body.put("items", items);
        } else {
            body.put("items", incomingItems);
        }

        // debug address & items (gọi nếu cần để xác định encoding lỗi)
        debugString("to_address", req.getToAddress());
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("[GHN] createOrder request JSON: {}", jsonBody);

            Mono<CreateOrderRes> mono = ghnClient.post()
                    .uri("/v2/shipping-order/create")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                    .bodyValue(jsonBody)
                    .exchangeToMono(response -> handleCreateResponse(response, body));

            return mono.block();
        } catch (Exception ex) {
            log.error("[GHN] createOrder unexpected error, requestBody={}", safeToJson(body), ex);
            return null;
        }
    }

    /**
     * Xử lý phản hồi GHN — nếu lỗi thì log body lỗi để debug
     */
    private Mono<CreateOrderRes> handleCreateResponse(ClientResponse response, Map<String, Object> requestBody) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(CreateOrderRes.class)
                    .doOnNext(res -> log.debug("[GHN] createOrder success: {}", safeToJson(res)));
        } else {
            return response.bodyToMono(String.class)
                    .flatMap(bodyStr -> {
                        // ghi log debug để phân tích encoding/giá trị trả về
                        debugString("ghn-response-body", bodyStr);

                        log.warn("[GHN] createOrder returned status {} body={} request={}",
                                response.statusCode(), bodyStr, safeToJson(requestBody));
                        try {
                            return Mono.just(objectMapper.readValue(bodyStr, CreateOrderRes.class));
                        } catch (Exception e) {
                            CreateOrderRes res = new CreateOrderRes();
                            res.setCode(String.valueOf(response.statusCode().value()));
                            res.setMessage("GHN error: " + bodyStr);
                            return Mono.just(res);
                        }
                    });
        }
    }

    private String safeToJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "<cannot serialize>"; }
    }

    @Override
    public OrderDetailRes getOrderDetail(String orderCode) {
        Map<String, Object> body = Map.of("order_code", orderCode);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return ghnClient.post()
                    .uri("/v2/shipping-order/detail")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(OrderDetailRes.class)
                    .block();
        } catch (Exception e) {
            log.error("[GHN] getOrderDetail error, body={}", safeToJson(body), e);
            return null;
        }
    }

    /**
     * Gọi API create order rồi trích order_code từ response
     */
    @Override
    public Optional<String> createOrderAndGetOrderCode(CreateOrderReq req) {
        try {
            CreateOrderRes res = createOrder(req);
            if (res == null || res.getData() == null) {
                log.warn("[GHN] createOrder returned null or empty data");
                return Optional.empty();
            }

            Map<?, ?> dataMap = objectMapper.convertValue(res.getData(), Map.class);
            if (dataMap == null || dataMap.isEmpty()) return Optional.empty();

            Object val = dataMap.get("order_code");
            if (val instanceof String s && !s.isBlank()) {
                return Optional.of(s.trim());
            }

            // nếu order_code nằm lồng bên trong
            if (dataMap.containsKey("order") && dataMap.get("order") instanceof Map nested) {
                Object v = ((Map<?, ?>) nested).get("order_code");
                if (v instanceof String s && !s.isBlank()) return Optional.of(s.trim());
            }

            log.warn("[GHN] createOrder response missing order_code field, data={}", dataMap);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("[GHN] createOrderAndGetOrderCode failed", ex);
            return Optional.empty();
        }
    }


    /**
     * In debug: string, codepoints, và UTF-8 bytes — giúp xác định nếu chữ đã hỏng trước khi gửi
     */
    private void debugString(String label, String s) {
        if (s == null) {
            log.debug("{} = null", label);
            return;
        }
        log.debug("{} = {}", label, s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            sb.append(String.format("U+%04X ", cp));
            i += Character.charCount(cp);
        }
        log.debug("{} codepoints: {}", label, sb.toString());
        try {
            byte[] bytes = s.getBytes("UTF-8");
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02X ", b));
            log.debug("{} UTF-8 bytes: {}", label, hex.toString());
        } catch (UnsupportedEncodingException e) {
            // won't happen for UTF-8 on standard JVM
        }
    }
}
