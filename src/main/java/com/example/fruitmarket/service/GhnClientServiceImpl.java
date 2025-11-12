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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * GHN client (đã fix):
 * - Mọi request GHN đều gắn headers: Token, ShopId, Content-Type, Accept
 * - ObjectMapper không escape non-ASCII
 * - Gửi body dưới dạng JSON string (đảm bảo encoding)
 * - Fallback items nếu thiếu
 * - Log chi tiết body lỗi khi GHN trả 4xx
 */
@Service
@Slf4j
public class GhnClientServiceImpl implements GhnClientService {

    private final WebClient ghnClient;   // dùng cho /available-services, /fee, /create, /detail
    private final WebClient webClient;   // dùng cho cancel (đã có sẵn trong code bạn)
    private final String shopId;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

    @Value("${ghn.base-url}")
    private String baseUrl;

    @Value("${ghn.token}")
    private String token;

    public GhnClientServiceImpl(
            WebClient ghnClient,
            @Value("${ghn.shop-id}") String shopId,
            WebClient webClient
    ) {
        this.ghnClient = ghnClient;
        this.shopId = shopId;
        this.webClient = webClient;
    }

    // ==========================
    // Available Services
    // ==========================
    @Override
    public AvailableServicesRes availableServices(int fromDistrictId, int toDistrictId) {
        Map<String, Object> body = new HashMap<>();
        try {
            body.put("shop_id", Integer.parseInt(shopId));
        } catch (NumberFormatException ex) {
            log.warn("Invalid ghn.shop-id value (not a number): {}", shopId);
            body.put("shop_id", shopId);
        }
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return ghnClient.post()
                    .uri("/v2/shipping-order/available-services")
                    .headers(h -> setCommonHeaders(h))
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(AvailableServicesRes.class)
                    .block();
        } catch (Exception e) {
            log.error("[GHN] availableServices error, body={}", safeToJson(body), e);
            return null;
        }
    }

    // ==========================
    // Calculate Fee
    // ==========================
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
                    .headers(h -> setCommonHeaders(h))
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(FeeRes.class)
                    .block();
        } catch (Exception e) {
            log.error("[GHN] calculateFee error, body={}", safeToJson(body), e);
            return null;
        }
    }

    // ==========================
    // Create Order
    // ==========================
    @Override
    public CreateOrderRes createOrder(CreateOrderReq req) {
        Map<String, Object> body = new HashMap<>();
        body.put("to_name", req.getToName());
        body.put("to_phone", req.getToPhone());
        body.put("to_address", req.getToAddress());
        body.put("to_ward_code", req.getToWardCode());
        body.put("to_district_id", req.getToDistrictId());
        body.put("from_district_id", req.getFromDistrictId());
        body.put("from_ward_code", req.getFromWardCode());
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

        // Items fallback
        Object incomingItems = req.getItems();
        if (incomingItems == null) {
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("name", "Hàng hóa");
            item.put("quantity", 1);
            item.put("price", req.getCodAmount() == null ? 0 : req.getCodAmount());
            item.put("weight", req.getWeight() == null ? 0 : req.getWeight());
            items.add(item);
            body.put("items", items);
        } else {
            body.put("items", incomingItems);
        }

        // Debug chuỗi địa chỉ (để xem encoding)
        debugString("to_address", req.getToAddress());

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            log.info("[GHN] createOrder request JSON: {}", jsonBody);

            Mono<CreateOrderRes> mono = ghnClient.post()
                    .uri("/v2/shipping-order/create")
                    .headers(h -> setCommonHeaders(h))
                    .bodyValue(jsonBody)
                    .exchangeToMono(response -> handleCreateResponse(response, body));

            return mono.block();
        } catch (Exception ex) {
            log.error("[GHN] createOrder unexpected error, requestBody={}", safeToJson(body), ex);
            return null;
        }
    }

    private Mono<CreateOrderRes> handleCreateResponse(ClientResponse response, Map<String, Object> requestBody) {
        if (response.statusCode().is2xxSuccessful()) {
            return response.bodyToMono(CreateOrderRes.class)
                    .doOnNext(res -> log.debug("[GHN] createOrder success: {}", safeToJson(res)));
        } else {
            return response.bodyToMono(String.class)
                    .flatMap(bodyStr -> {
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

    // ==========================
    // Order Detail
    // ==========================
    @Override
    public OrderDetailRes getOrderDetail(String orderCode) {
        Map<String, Object> body = Map.of("order_code", orderCode);
        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            return ghnClient.post()
                    .uri("/v2/shipping-order/detail")
                    .headers(h -> setCommonHeaders(h))
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(OrderDetailRes.class)
                    .block();
        } catch (Exception e) {
            log.error("[GHN] getOrderDetail error, body={}", safeToJson(body), e);
            return null;
        }
    }

    // ==========================
    // Helper: create + get order_code
    // ==========================
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

    // ==========================
    // Cancel (giữ như bạn đã làm, có headers đầy đủ)
    // ==========================
    @Override
    public void cancelOrder(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("orderCode rỗng/không hợp lệ");
        }
        final String cancelV2Switch = normalize(baseUrl) + "/v2/switch-status/cancel";
        final String cancelV2Single = normalize(baseUrl) + "/v2/shipping-order/cancel";

        // Cách 1: /v2/switch-status/cancel
        Map<String, Object> bodyArray = Map.of("order_codes", List.of(orderCode));
        try {
            GhnCancelResp resp = webClient.post()
                    .uri(cancelV2Switch)
                    .headers(this::setCommonHeaders)
                    .bodyValue(bodyArray)
                    .retrieve()
                    .bodyToMono(GhnCancelResp.class)
                    .block();

            if (!isSuccess(resp)) {
                String msg = "GHN cancel (switch-status) trả về lỗi: " + safeMsg(resp);
                log.warn(msg);
            } else {
                log.info("Huỷ GHN thành công (switch-status) cho orderCode={}", orderCode);
            }
            return;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 404 || ex.getStatusCode().value() == 405) {
                log.info("Endpoint switch-status/cancel không khả dụng, thử fallback shipping-order/cancel");
            } else {
                log.warn("GHN cancel switch-status lỗi {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            }
        } catch (Exception ex) {
            log.warn("GHN cancel switch-status gặp lỗi: {}", ex.getMessage(), ex);
        }

        // Cách 2 (fallback): /v2/shipping-order/cancel
        Map<String, Object> bodySingle = Map.of("order_code", orderCode);
        try {
            GhnCancelResp resp = webClient.post()
                    .uri(cancelV2Single)
                    .headers(this::setCommonHeaders)
                    .bodyValue(bodySingle)
                    .retrieve()
                    .bodyToMono(GhnCancelResp.class)
                    .block();

            if (!isSuccess(resp)) {
                String msg = "GHN cancel (shipping-order) trả về lỗi: " + safeMsg(resp);
                log.warn(msg);
            } else {
                log.info("Huỷ GHN thành công (shipping-order) cho orderCode={}", orderCode);
            }
        } catch (WebClientResponseException ex) {
            log.warn("GHN cancel shipping-order lỗi {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.warn("GHN cancel shipping-order gặp lỗi: {}", ex.getMessage(), ex);
        }
    }

    // ==========================
    // Private helpers
    // ==========================
    private void setCommonHeaders(HttpHeaders h) {
        h.set("Token", token);
        if (shopId != null) h.set("ShopId", String.valueOf(shopId));
        h.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    private String normalize(String url) {
        if (url == null || url.isBlank()) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean isSuccess(GhnCancelResp resp) {
        if (resp == null) return false;
        if (resp.getCode() != null) return resp.getCode() == 200;
        if (resp.getSuccess() != null) return resp.getSuccess();
        return resp.getData() != null;
    }

    private String safeMsg(GhnCancelResp resp) {
        if (resp == null) return "<null>";
        if (resp.getMessage() != null) return resp.getMessage();
        if (resp.getError() != null) return resp.getError();
        return String.valueOf(resp);
    }

    private String safeToJson(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (Exception e) { return "<cannot serialize>"; }
    }

    /** In debug codepoints/UTF-8 để kiểm tra lỗi encoding nếu có */
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
        } catch (UnsupportedEncodingException ignored) {}
    }
}
