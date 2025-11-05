package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.CreateOrderReq;
import com.example.fruitmarket.dto.OrderDetailRes;
import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.enums.GhnStatus;
import com.example.fruitmarket.enums.OrderStauts;
import com.example.fruitmarket.enums.PricingMethod;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.OrderRepo;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductService productService;

    @Autowired
    private GhnClientService ghnClientService;

    @Value("${ghn.from-district-id}")
    private int fromDistrictId;

    @Override
    @Transactional
    public Order createOrder(
            HttpSession session,
            ProductVariant variant,
            Integer quantity,
            Long addressId,
            String paymentMethod,
            BigDecimal shippingFee,
            Integer serviceId
    ) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("User not logged in");

        User_detail ud = userService.findUserDetalById(addressId);
        if (ud == null || ud.getDistrict() == null || ud.getWard() == null) {
            throw new IllegalArgumentException("Invalid shipping address (missing ward/district)");
        }

        Order order = new Order();
        order.setUsers(user);
        order.setAddress(ud.getAddress());
        order.setPhoneNumber(ud.getPhone());
        try {
            order.setPricingMethod(PricingMethod.valueOf(paymentMethod));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
        }
        order.setOrderStauts(OrderStauts.PENDING);

        // item
        OrderItem oi = new OrderItem();
        oi.setQuanity(quantity != null && quantity > 0 ? quantity : 1);
        oi.setProductVariant(variant);
        oi.setPrice(variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO);

        order.setOrderItemList(new ArrayList<>());
        order.getOrderItemList().add(oi);

        // totals
        BigDecimal goodsTotal = oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuanity()));
        BigDecimal safeShip = (shippingFee != null && shippingFee.signum() >= 0) ? shippingFee : BigDecimal.ZERO;
        order.setTotalPrice(goodsTotal.add(safeShip));
        order.setTotalQuantity(oi.getQuanity());
        order.setShippingFee(safeShip);
        order.setGhnStatus(GhnStatus.READY_TO_PICK);

        Order saved = orderRepo.save(order);

        // === GHN create order (best-effort) ===
        try {
            var svRes = ghnClientService.availableServices(fromDistrictId, ud.getDistrict().getDistrictId());
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int useServiceId = (serviceId != null && serviceId > 0)
                        ? serviceId
                        : svRes.getData().get(0).getServiceId();

                long weight = Math.max(1, saved.getTotalQuantity()) * 500L;
                int length = 20, width = 15, height = 10;

                int cod = (saved.getPricingMethod() == PricingMethod.COD)
                        ? saved.getTotalPrice().intValue()
                        : 0;

                CreateOrderReq req = new CreateOrderReq();
                req.setToName(user.getUsername());
                req.setToPhone(ud.getPhone());
                req.setToAddress(ud.getAddress());
                req.setToWardCode(ud.getWard().getWardCode());
                req.setToDistrictId(ud.getDistrict().getDistrictId());
                req.setServiceId(useServiceId);
                req.setWeight(weight);
                req.setLength(length);
                req.setWidth(width);
                req.setHeight(height);
                req.setCodAmount(cod);
                req.setClientOrderCode("ORD-" + saved.getId());

                // ✅ thêm các dòng bắt buộc
                req.setPayment_type_id(1); // shop trả ship
                req.setRequired_note("KHONGCHOXEMHANG");
                req.setNote("Đơn hàng FruitMarket #" + saved.getId());

                // ✅ thêm items
                List<CreateOrderReq.Item> items = new ArrayList<>();
                CreateOrderReq.Item item = new CreateOrderReq.Item();
                item.setName(variant.getProduct().getProductName());
                item.setQuantity(quantity);
                items.add(item);
                req.setItems(items);

                // BẮT exception chi tiết từ WebClient để log body (400/422) mà không làm đổ TX
                try {
                    Optional<String> optCode = ghnClientService.createOrderAndGetOrderCode(req);
                    if (optCode.isPresent()) {
                        String code = optCode.get();
                        saved.setGhnOrderCode(code);

                        OrderDetailRes detail = null;
                        try {
                            detail = ghnClientService.getOrderDetail(code);
                        } catch (WebClientResponseException wcre) {
                            log.warn("Failed to fetch GHN order detail for code {}: status={} body={}", code, wcre.getStatusCode(), wcre.getResponseBodyAsString());
                        } catch (Exception e) {
                            log.warn("Failed to fetch GHN order detail for code {}: {}", code, e.getMessage(), e);
                        }

                        if (detail != null && detail.getData() != null) {
                            saved.setGhnStatus(GhnStatus.READY_TO_PICK);
                        }
                        orderRepo.saveAndFlush(saved);
                        log.info("Saved GHN order code {} for local order {}", code, saved.getId());
                    } else {
                        log.warn("[GHN] createOrder: no orderCode returned for local order {}", saved.getId());
                    }
                } catch (WebClientResponseException wcre) {
                    // log chi tiết body từ GHN (rất hữu ích để debug payload thiếu trường/thiếu header)
                    String body = "<no body>";
                    try { body = wcre.getResponseBodyAsString(); } catch (Exception ignore) {}
                    log.warn("[GHN] createOrder failed with status {} and body: {}", wcre.getStatusCode(), body);
                    // không ném tiếp: giữ best-effort (local order đã lưu)
                } catch (Exception ex) {
                    log.error("Unexpected error while creating GHN order for local order " + saved.getId(), ex);
                }
            } else {
                log.warn("[GHN] no available service for route {} -> {}", fromDistrictId, ud.getDistrict().getDistrictId());
            }
        } catch (Exception ex) {
            // bảo vệ an toàn: bất kỳ lỗi nào ở bước kiểm tra service cũng không làm fail toàn bộ flow
            log.warn("Failed to check available GHN services (best-effort): {}", ex.getMessage(), ex);
        }

        return saved;
    }

    @Override
    @Transactional
    public Long createOrderFromCart(OrderRequest orderReq, HttpSession session) {
        // ===== 0) user & address =====
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("User not logged in");

        Long addressId = orderReq.getAddressId();
        if (addressId == null) throw new IllegalArgumentException("AddressId is required");

        User_detail userDetail = userService.findUserDetalById(addressId);
        if (userDetail == null) throw new IllegalArgumentException("Invalid addressId: " + addressId);
        if (userDetail.getDistrict() == null || userDetail.getWard() == null)
            throw new IllegalArgumentException("Address missing district/ward");

        // ===== 1) build Order & items =====
        Order order = new Order();
        order.setUsers(user);
        order.setAddress(userDetail.getAddress());
        order.setPhoneNumber(userDetail.getPhone());

        try {
            order.setPricingMethod(PricingMethod.valueOf(orderReq.getPaymentMethod()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid payment method: " + orderReq.getPaymentMethod());
        }
        order.setOrderStauts(OrderStauts.PENDING);

        if (orderReq.getItems() == null || orderReq.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal goodsTotal = BigDecimal.ZERO;
        int totalQty = 0;

        for (OrderRequest.OrderItem reqItem : orderReq.getItems()) {
            ProductVariant pv = null;

            if (reqItem.getVariantId() != null) {
                pv = productService.findProductVariantById(reqItem.getVariantId());
            } else if (reqItem.getProductId() != null) {
                // TODO: thay bằng method lấy variant mặc định theo productId nếu có
                pv = productService.findProductVariantById(reqItem.getProductId());
            }
            if (pv == null) throw new IllegalArgumentException("ProductVariant not found");

            int qty = (reqItem.getQuantity() != null && reqItem.getQuantity() > 0) ? reqItem.getQuantity() : 1;

            OrderItem oi = new OrderItem();
            oi.setProductVariant(pv);
            oi.setQuanity(qty);
            oi.setPrice(reqItem.getPrice() != null ? reqItem.getPrice() : pv.getPrice());
            // nếu cần: oi.setOrder(order);

            BigDecimal price = oi.getPrice() != null ? oi.getPrice() : BigDecimal.ZERO;
            goodsTotal = goodsTotal.add(price.multiply(BigDecimal.valueOf(qty)));
            totalQty += qty;

            items.add(oi);
        }

        order.setOrderItemList(new ArrayList<>());
        order.getOrderItemList().addAll(items);
        order.setTotalQuantity(totalQty);

        // ===== 2) TÍNH PHÍ SHIP GHN & CỘNG VÀO TỔNG =====
        BigDecimal shippingFee = BigDecimal.ZERO;
        try {
            int toDistrictId = userDetail.getDistrict().getDistrictId();
            String toWardCode = userDetail.getWard().getWardCode();

            var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int serviceId = svRes.getData().get(0).getServiceId();

                int weight = (int) (Math.max(1, totalQty) * 500L);
                int length = 20, width = 15, height = 10;

                var feeRes = ghnClientService.calculateFee(
                        fromDistrictId, toDistrictId, toWardCode,
                        serviceId, weight, length, width, height,
                        goodsTotal.intValue()
                );
                Integer fee = (feeRes != null && feeRes.getData() != null) ? feeRes.getData().getTotal() : null;
                shippingFee = BigDecimal.valueOf(fee != null ? fee : 0);
            }
        } catch (Exception ignore) {
            // không chặn flow khi GHN lỗi -> để ship = 0
            log.warn("Failed to calculate GHN shipping fee, default to 0", ignore);
        }

        order.setShippingFee(shippingFee);
        order.setTotalPrice(goodsTotal.add(shippingFee));

        // ===== 3) LƯU ĐƠN =====
        Order saved = orderRepo.save(order);

        // ===== 4) (tuỳ chọn) tạo đơn GHN sau khi lưu thành công =====
        try {
            String toName = user.getUsername();
            String toPhone = userDetail.getPhone();
            String toAddr  = userDetail.getAddress();
            String toWardCode   = userDetail.getWard().getWardCode();
            Integer toDistrictId= userDetail.getDistrict().getDistrictId();

            var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int serviceId = svRes.getData().get(0).getServiceId();

                long weight = Math.max(1, order.getTotalQuantity()) * 500L;
                int length = 20, width = 15, height = 10;

                int cod = order.getPricingMethod() == PricingMethod.COD
                        ? order.getTotalPrice().intValue()
                        : 0;

                CreateOrderReq req = new CreateOrderReq();
                req.setToName(toName);
                req.setToPhone(toPhone);
                req.setToAddress(toAddr);
                req.setToWardCode(toWardCode);
                req.setToDistrictId(toDistrictId);
                req.setServiceId(serviceId);
                req.setWeight(weight);
                req.setLength(length);
                req.setWidth(width);
                req.setHeight(height);
                req.setCodAmount(cod);
                req.setClientOrderCode("ORD-" + saved.getId());

                try {
                    Optional<String> optCode = ghnClientService.createOrderAndGetOrderCode(req);
                    if (optCode.isPresent()) {
                        saved.setGhnOrderCode(optCode.get());
                        var detail = (OrderDetailRes) null;
                        try {
                            detail = ghnClientService.getOrderDetail(saved.getGhnOrderCode());
                        } catch (WebClientResponseException wcre) {
                            log.warn("Failed to fetch GHN order detail for code {}: status={} body={}", saved.getGhnOrderCode(), wcre.getStatusCode(), wcre.getResponseBodyAsString());
                        } catch (Exception e) {
                            log.warn("Failed to fetch GHN order detail for code {}: {}", saved.getGhnOrderCode(), e.getMessage(), e);
                        }

                        if (detail != null && detail.getData() != null) {
                            saved.setGhnStatus(GhnStatus.READY_TO_PICK);
                        }
                        orderRepo.saveAndFlush(saved);
                        log.info("Saved GHN order code {} for local order {}", saved.getGhnOrderCode(), saved.getId());
                    } else {
                        log.warn("[GHN] createOrder returned no order code for order {}", saved.getId());
                    }
                } catch (WebClientResponseException wcre) {
                    String body = "<no body>";
                    try { body = wcre.getResponseBodyAsString(); } catch (Exception ignore) {}
                    log.warn("[GHN] createOrder failed with status {} and body: {}", wcre.getStatusCode(), body);
                } catch (Exception ex) {
                    log.error("Create GHN failed", ex);
                }
            }
        } catch (Exception ex) {
            log.warn("Create GHN failed (service check)", ex);
        }

        return saved.getId();
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với ID: " + id));
    }

    @Override
    public void updateOrder(Order order) {
        orderRepo.save(order);
    }

    @Override
    public List<Order> getOrdersOfUser(HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("Bạn cần đăng nhập trước.");
        return orderRepo.findAllByUsersOrderByIdDesc(user);
    }

    @Override
    public Order getOrderDetailForUser(Long orderId, HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("Bạn cần đăng nhập trước.");

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        if (order.getUsers() == null
                || !Objects.equals(order.getUsers().getId(), user.getId())) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này.");
        }
        return order;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }

    @Override
    public void attachShippingCode(Long orderId, String ghnOrderCode) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));
        order.setGhnOrderCode(ghnOrderCode);
        orderRepo.save(order);
    }

    @Override
    @Transactional
    public boolean updateFromGhnCallback(long clientOrderCode,
                                         String ghnOrderCode,
                                         GhnStatus ghnStatus,
                                         Integer codAmount) {

        Order order = orderRepo.findById(clientOrderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng: " + clientOrderCode));

        // Cập nhật mã GHN nếu cần
        if (ghnOrderCode != null && !ghnOrderCode.isBlank()
                && (order.getGhnOrderCode() == null || !ghnOrderCode.equals(order.getGhnOrderCode()))) {
            order.setGhnOrderCode(ghnOrderCode);
        }

        // Ghi nhận trạng thái GHN mới nhất
        order.setGhnStatus(ghnStatus);

        // Map GHN → OrderStatus theo yêu cầu
        switch (ghnStatus) {
            case READY_TO_PICK -> {
                order.setOrderStauts(OrderStauts.PENDING);
            }

            case DELIVERING -> {
                order.setOrderStauts(OrderStauts.SHIPPING);
            }

            case DELIVERED -> {
                // ✅ Nếu là COD và chưa thanh toán → tự động hoàn tất & đánh dấu paid
                if ("COD".equalsIgnoreCase(String.valueOf(order.getPricingMethod())) && !order.isPaid()) {
                    order.setOrderStauts(OrderStauts.COMPLETED);
                    order.setPaid(true);
                    log.info("Đơn {} (COD) giao thành công, tự động set COMPLETED + PAID.", clientOrderCode);
                }
                // Nếu đã thanh toán trước → hoàn tất
                else if (order.isPaid()) {
                    order.setOrderStauts(OrderStauts.COMPLETED);
                    log.info("Đơn {} đã thanh toán, GHN DELIVERED -> COMPLETED.", clientOrderCode);
                }
                // Ngược lại → đánh dấu đã giao (chưa thanh toán)
                else {
                    order.setOrderStauts(OrderStauts.SHIPPED);
                    log.info("Đơn {} chưa thanh toán, GHN DELIVERED -> SHIPPED.", clientOrderCode);
                }
            }

            default -> {
                log.info("GHN status {} không thay đổi OrderStauts (giữ {}).",
                        ghnStatus, order.getOrderStauts());
            }
        }

        orderRepo.save(order);
        return true;
    }



    // ===== helper =====
    private String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private Long parseOrderIdFromClientOrderCode(String code) {
        if (code == null) throw new IllegalArgumentException("clientOrderCode null");
        String c = code.trim();
        if (c.startsWith("ORD-")) {
            try {
                return Long.parseLong(c.substring(4));
            } catch (NumberFormatException ignored) {}
        }
        try {
            return Long.parseLong(c);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Không parse được orderId từ clientOrderCode: " + code);
        }
    }

    @Override
    @Transactional
    public void updateShippingFee(Long orderId, BigDecimal fee) {
        if (orderId == null || orderId <= 0) throw new IllegalArgumentException("orderId không hợp lệ");
        BigDecimal safeFee = (fee != null && fee.signum() >= 0) ? fee : BigDecimal.ZERO;

        int updated = orderRepo.updateShippingFee(orderId, safeFee);
        if (updated == 0) throw new IllegalStateException("Không tìm thấy order để cập nhật phí ship: " + orderId);
    }

    @Override
    @Transactional
    public void addShippingToTotal(Long orderId, BigDecimal fee) {
        orderRepo.addShippingToTotal(orderId, (fee != null) ? fee : BigDecimal.ZERO);
    }
}
