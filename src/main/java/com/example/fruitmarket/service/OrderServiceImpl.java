package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.CreateOrderReq;
import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.enums.GhnStatus;
import com.example.fruitmarket.enums.OrderStauts;
import com.example.fruitmarket.enums.PricingMethod;
import com.example.fruitmarket.enums.Units;
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
import java.util.*;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired private UserService userService;
    @Autowired private OrderRepo orderRepo;
    @Autowired private ProductService productService;
    @Autowired private GhnClientService ghnClientService;

    @Value("${ghn.from-district-id}")
    private int fromDistrictId;
    @Value("${ghn.from-ward-code}")
    private String fromWardCode;

    /* ============================
     * createOrder (OVERLOAD 1) — tương thích code cũ
     * ============================ */
    @Transactional
    @Override
    public Order createOrder(HttpSession session,
                             ProductVariant variant,
                             Integer quantity,
                             Long addressId,
                             String paymentMethod) {
        return createOrder(session, variant, quantity, addressId, paymentMethod, null, null);
    }

    /* =============================================
     * createOrder (OVERLOAD 2) — bản đầy đủ
     * ============================================= */
    @Override
    @Transactional
    public Order createOrder(HttpSession session,
                             ProductVariant variant,
                             Integer quantity,
                             Long addressId,
                             String paymentMethod,
                             BigDecimal shippingFee,
                             Integer serviceId) {

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
        int q = (quantity != null && quantity > 0) ? quantity : 1;
        oi.setQuantity(q);
        oi.setProductVariant(variant);
        oi.setPrice(variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO);

        order.setOrderItemList(new ArrayList<>());
        order.getOrderItemList().add(oi);

        // totals
        BigDecimal goodsTotal = oi.getPrice().multiply(BigDecimal.valueOf(q));
        BigDecimal safeShip = (shippingFee != null && shippingFee.signum() >= 0) ? shippingFee : BigDecimal.ZERO;

        order.setShippingFee(safeShip);
        order.setTotalPrice(goodsTotal.add(safeShip));
        order.setTotalQuantity(q);
        order.setGhnStatus(GhnStatus.READY_TO_PICK);

        Order saved = orderRepo.save(order);

        // === GHN create order (best-effort) ===
        try {
            var svRes = ghnClientService.availableServices(fromDistrictId, ud.getDistrict().getDistrictId());
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int useServiceId = (serviceId != null && serviceId > 0)
                        ? serviceId
                        : svRes.getData().get(0).getServiceId();

                long weightGram = Math.max(1, saved.getTotalQuantity()) * 500L; // 500g/đơn vị
                if (weightGram < 100) weightGram = 100;

                int length = 20, width = 15, height = 10;

                int cod = (saved.getPricingMethod() == PricingMethod.COD)
                        ? saved.getTotalPrice().intValue() : 0;

                CreateOrderReq req = new CreateOrderReq();
                req.setToName(user.getUsername());
                req.setToPhone(ud.getPhone());
                req.setToAddress(normalizeVi(ud.getAddress()));
                req.setToWardCode(ud.getWard().getWardCode());
                req.setFromWardCode(fromWardCode);
                req.setFromDistrictId(fromDistrictId);
                req.setToDistrictId(ud.getDistrict().getDistrictId());
                req.setServiceId(useServiceId);
                req.setWeight(weightGram);
                req.setLength(length);
                req.setWidth(width);
                req.setHeight(height);
                req.setCodAmount(cod);
                req.setClientOrderCode("ORD-" + saved.getId());

                // Bắt buộc GHN
                req.setPayment_type_id(1); // shop trả ship
                req.setRequired_note("KHONGCHOXEMHANG");
                req.setNote("Đơn hàng FruitMarket #" + saved.getId());

                // Items
                List<CreateOrderReq.Item> items = new ArrayList<>();
                CreateOrderReq.Item item = new CreateOrderReq.Item();
                item.setName(variant.getProduct().getProductName());
                item.setQuantity(q);
                items.add(item);
                req.setItems(items);

                try {
                    Optional<String> optCode = ghnClientService.createOrderAndGetOrderCode(req);
                    if (optCode.isPresent()) {
                        String code = optCode.get();
                        saved.setGhnOrderCode(code);

                        try {
                            var detail = ghnClientService.getOrderDetail(code);
                            if (detail != null && detail.getData() != null) {
                                saved.setGhnStatus(GhnStatus.READY_TO_PICK);
                            }
                        } catch (WebClientResponseException wcre) {
                            log.warn("Failed to fetch GHN order detail for code {}: status={} body={}",
                                    code, wcre.getStatusCode(), wcre.getResponseBodyAsString());
                        } catch (Exception e) {
                            log.warn("Failed to fetch GHN order detail for code {}: {}", code, e.getMessage(), e);
                        }

                        orderRepo.saveAndFlush(saved);
                        log.info("Saved GHN order code {} for local order {}", code, saved.getId());
                    } else {
                        log.warn("[GHN] createOrder: no orderCode returned for local order {}", saved.getId());
                    }
                } catch (WebClientResponseException wcre) {
                    String body = "<no body>";
                    try { body = wcre.getResponseBodyAsString(); } catch (Exception ignore) {}
                    log.warn("[GHN] createOrder failed with status {} and body: {}", wcre.getStatusCode(), body);
                } catch (Exception ex) {
                    log.error("Unexpected error while creating GHN order for local order " + saved.getId(), ex);
                }
            } else {
                log.warn("[GHN] no available service for route {} -> {}", fromDistrictId, ud.getDistrict().getDistrictId());
            }
        } catch (Exception ex) {
            log.warn("Failed to check available GHN services (best-effort): {}", ex.getMessage(), ex);
        }

        return saved;
    }

    /* =====================================================
     * createOrderFromCart — HỖ TRỢ CẢ quantity & weight (kg)
     * Tính ship GHN theo cân nặng thực tế (kg ⇒ gram) + ước lượng 500g/đơn vị
     * ===================================================== */
    @Override
    @Transactional
    public Long createOrderFromCart(OrderRequest orderReq, HttpSession session) {
        // 0) user & address
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("User not logged in");

        Long addressId = orderReq.getAddressId();
        if (addressId == null) throw new IllegalArgumentException("AddressId is required");

        User_detail userDetail = userService.findUserDetalById(addressId);
        if (userDetail == null) throw new IllegalArgumentException("Invalid addressId: " + addressId);
        if (userDetail.getDistrict() == null || userDetail.getWard() == null)
            throw new IllegalArgumentException("Address missing district/ward");

        // 1) build Order & items
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

        int totalQtyPiece = 0;      // tổng số "cái"
        double totalWeightKg = 0.0; // tổng số kg

        for (OrderRequest.OrderItem reqItem : orderReq.getItems()) {
            ProductVariant pv = null;
            if (reqItem.getVariantId() != null) {
                pv = productService.findProductVariantById(reqItem.getVariantId());
            } else if (reqItem.getProductId() != null) {
                // TODO: nếu có method lấy variant mặc định theo productId thì dùng; tạm fallback
                pv = productService.findProductVariantById(reqItem.getProductId());
            }
            if (pv == null) throw new IllegalArgumentException("ProductVariant not found");

            BigDecimal unitPrice = (reqItem.getPrice() != null ? reqItem.getPrice() : pv.getPrice());
            if (unitPrice == null) unitPrice = BigDecimal.ZERO;

            Integer quantity = reqItem.getQuantity();
            Double  weightKg = reqItem.getWeight();

            OrderItem oi = new OrderItem();
            oi.setProductVariant(pv);
            oi.setPrice(unitPrice);

            if (weightKg != null && weightKg > 0) {
                // Bán theo kg
                oi.setWeight(weightKg);
                oi.setQuantity(null);
                oi.setUnit(Units.KILOGRAM); // ✅ Thêm dòng này
                goodsTotal = goodsTotal.add(unitPrice.multiply(BigDecimal.valueOf(weightKg)));
                totalWeightKg += weightKg;
            } else {
                // Bán theo cái
                int q = (quantity != null && quantity > 0) ? quantity : 1;
                oi.setQuantity(q);
                oi.setWeight(null);
                oi.setUnit(Units.PIECE); // ✅ Thêm dòng này
                goodsTotal = goodsTotal.add(unitPrice.multiply(BigDecimal.valueOf(q)));
                totalQtyPiece += q;
            }


            items.add(oi);
        }

        order.setOrderItemList(new ArrayList<>(items));
        order.setTotalQuantity(totalQtyPiece);
        // Nếu entity Order có trường totalWeight(Double) thì set; nếu không, có thể bỏ.
        try { order.setTotalWeight(totalWeightKg); } catch (Throwable ignore) {}

        // 2) TÍNH PHÍ SHIP GHN (có trọng lượng thực)
        BigDecimal shippingFee = BigDecimal.ZERO;
        try {
            int toDistrictId = userDetail.getDistrict().getDistrictId();
            String toWardCode = userDetail.getWard().getWardCode();

            var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int serviceId = svRes.getData().get(0).getServiceId();

                long weightGram =
                        Math.max(0, Math.round(totalWeightKg * 1000))
                                + (long) Math.max(0, totalQtyPiece) * 500L;
                if (weightGram < 100) weightGram = 100;

                int length = 20, width = 15, height = 10;

                var feeRes = ghnClientService.calculateFee(
                        fromDistrictId, toDistrictId, toWardCode,
                        serviceId, (int) weightGram, length, width, height,
                        goodsTotal.intValue()
                );
                Integer fee = (feeRes != null && feeRes.getData() != null) ? feeRes.getData().getTotal() : null;
                shippingFee = BigDecimal.valueOf(fee != null ? fee : 0);
            }
        } catch (Exception ex) {
            log.warn("Failed to calculate GHN shipping fee, default to 0", ex);
        }

        order.setShippingFee(shippingFee);
        order.setTotalPrice(goodsTotal.add(shippingFee));

        // 3) LƯU ĐƠN
        Order saved = orderRepo.save(order);

        // 4) (tuỳ chọn) tạo đơn GHN sau khi lưu thành công
        try {
            String toName       = user.getUsername();
            String toPhone      = userDetail.getPhone();
            String toAddr       = userDetail.getAddress();
            String toWardCode   = userDetail.getWard().getWardCode();
            Integer toDistrictId= userDetail.getDistrict().getDistrictId();

            var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int serviceId = svRes.getData().get(0).getServiceId();

                long weightGram =
                        Math.max(0, Math.round(totalWeightKg * 1000))
                                + (long) Math.max(0, saved.getTotalQuantity()) * 500L;
                if (weightGram < 100) weightGram = 100;

                int length = 20, width = 15, height = 10;

                int cod = saved.getPricingMethod() == PricingMethod.COD
                        ? saved.getTotalPrice().intValue()
                        : 0;

                CreateOrderReq req = new CreateOrderReq();
                req.setToName(toName);
                req.setToPhone(toPhone);
                req.setToAddress(toAddr);
                req.setToWardCode(toWardCode);
                req.setToDistrictId(toDistrictId);
                req.setServiceId(serviceId);
                req.setWeight(weightGram);
                req.setLength(length);
                req.setWidth(width);
                req.setHeight(height);
                req.setCodAmount(cod);
                req.setClientOrderCode("ORD-" + saved.getId());

                // Bắt buộc GHN
                req.setPayment_type_id(1); // shop trả ship
                req.setRequired_note("KHONGCHOXEMHANG");
                req.setNote("Đơn hàng FruitMarket #" + saved.getId());

                List<CreateOrderReq.Item> itemsGhn = new ArrayList<>();
                for (OrderItem it : saved.getOrderItemList()) {
                    CreateOrderReq.Item item = new CreateOrderReq.Item();
                    item.setName(it.getProductVariant().getProduct().getProductName());
                    // GHN cần quantity int; với hàng theo kg gửi 1 để hợp lệ
                    item.setQuantity(it.getQuantity() != null ? it.getQuantity() : 1);
                    itemsGhn.add(item);
                }
                req.setItems(itemsGhn);

                try {
                    Optional<String> optCode = ghnClientService.createOrderAndGetOrderCode(req);
                    if (optCode.isPresent()) {
                        saved.setGhnOrderCode(optCode.get());
                        try {
                            var detail = ghnClientService.getOrderDetail(saved.getGhnOrderCode());
                            if (detail != null && detail.getData() != null) {
                                saved.setGhnStatus(GhnStatus.READY_TO_PICK);
                            }
                        } catch (WebClientResponseException wcre) {
                            log.warn("Failed to fetch GHN order detail for code {}: status={} body={}",
                                    saved.getGhnOrderCode(), wcre.getStatusCode(), wcre.getResponseBodyAsString());
                        } catch (Exception e) {
                            log.warn("Failed to fetch GHN order detail for code {}: {}", saved.getGhnOrderCode(), e.getMessage(), e);
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

    /* ===== CRUD/Query ===== */

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

        // 1) GHN code
        if (ghnOrderCode != null && !ghnOrderCode.isBlank()
                && (order.getGhnOrderCode() == null || !ghnOrderCode.equals(order.getGhnOrderCode()))) {
            order.setGhnOrderCode(ghnOrderCode);
        }

        // 2) Chặn callback đi lùi
        GhnStatus oldGhn = order.getGhnStatus();
        if (oldGhn != null) {
            int oldRank = GHN_RANK.getOrDefault(oldGhn, 0);
            int newRank = GHN_RANK.getOrDefault(ghnStatus, 0);
            if (newRank < oldRank) {
                log.warn("BỎ QUA callback GHN: {} < {} cho đơn {}", ghnStatus, oldGhn, clientOrderCode);
                return false; // không cập nhật gì
            }
        }

        // 3) Ghi nhận GHN status (đã qua kiểm tra không đi lùi)
        order.setGhnStatus(ghnStatus);

        // 4) Map GHN -> OrderStatus (chỉ tiến tới, không kéo lùi OrderStatus)
        switch (ghnStatus) {
            case READY_TO_PICK -> {
                // chỉ set PENDING nếu chưa vượt qua PENDING
                if (order.getOrderStauts() == null ||
                        order.getOrderStauts().rank() <= OrderStauts.PENDING.rank()) {
                    order.setOrderStauts(OrderStauts.PENDING);
                }
            }
            case DELIVERING -> {
                // khi đang giao thì coi như SHIPPING
                if (order.getOrderStauts() == null ||
                        order.getOrderStauts().rank() < OrderStauts.SHIPPING.rank()) {
                    order.setOrderStauts(OrderStauts.SHIPPING);
                }
            }
            case DELIVERED -> {
                // khi đã giao: hoàn tất/đã thu COD thì COMPLETED, chưa thu thì SHIPPED
                boolean isCOD = "COD".equalsIgnoreCase(String.valueOf(order.getPricingMethod()));
                if (isCOD && !order.isPaid()) {
                    order.setOrderStauts(OrderStauts.COMPLETED);
                    order.setPaid(true);
                    log.info("Đơn {} (COD) giao thành công, set COMPLETED + PAID.", clientOrderCode);
                } else if (order.isPaid()) {
                    order.setOrderStauts(OrderStauts.COMPLETED);
                    log.info("Đơn {} đã thanh toán, GHN DELIVERED -> COMPLETED.", clientOrderCode);
                } else {
                    // chưa thanh toán online + GHN báo delivered: đánh dấu đã giao
                    if (order.getOrderStauts() == null ||
                            order.getOrderStauts().rank() < OrderStauts.SHIPPED.rank()) {
                        order.setOrderStauts(OrderStauts.SHIPPED);
                    }
                    log.info("Đơn {} chưa thanh toán, GHN DELIVERED -> SHIPPED.", clientOrderCode);
                }
            }
            default -> log.info("GHN status {} không đổi OrderStauts (giữ {}).", ghnStatus, order.getOrderStauts());
        }

        orderRepo.save(order);
        return true;
    }

    /* ===== Shipping fee helpers ===== */

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

    @Override
    @Transactional
    public void cancelOrderForUser(Long orderId, HttpSession session) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("Bạn cần đăng nhập trước.");

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        // Quyền: chỉ chủ đơn hoặc admin
        boolean isOwner = order.getUsers() != null && Objects.equals(order.getUsers().getId(), user.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(String.valueOf(user.getRole()));
        if (!isOwner && !isAdmin) throw new IllegalStateException("Bạn không có quyền huỷ đơn hàng này.");

        // Không cho huỷ khi không thoả điều kiện
        if (!canCancel(order)) {
            throw new IllegalStateException("Đơn hiện tại không thể huỷ (trạng thái: " + order.getOrderStauts() + ").");
        }

        // Best-effort huỷ bên GHN (nếu có mã)
        String ghnCode = order.getGhnOrderCode();
        if (ghnCode != null && !ghnCode.isBlank()) {
            try {
                ghnClientService.cancelOrder(ghnCode);
                log.info("GHN cancel OK for order {}, code {}", orderId, ghnCode);
            } catch (WebClientResponseException wcre) {
                log.warn("Huỷ GHN thất bại: status={} body={}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            } catch (Exception ex) {
                log.warn("Huỷ GHN thất bại: {}", ex.getMessage(), ex);
            }
        }

        // Cập nhật trạng thái nội bộ
        order.setOrderStauts(OrderStauts.CANCELLED); // ❗Nếu enum bạn là CANCELLED thì đổi tại đây
        order.setGhnStatus(GhnStatus.CANCEL);       // nếu có giá trị tương ứng
        // Nếu cần: xử lý hoàn tiền tại đây (tuỳ cổng thanh toán)

        orderRepo.save(order);
    }

    /** Chỉ cho phép huỷ khi vẫn còn ở giai đoạn trước giao hàng.
     *  - KHÔNG HUỶ nếu Order ở: SHIPPING, SHIPPED, COMPLETED, CANCELED
     *  - KHÔNG HUỶ nếu GHN ở: DELIVERING, DELIVERED
     *  -> Cho huỷ khi: PENDING hoặc GHN READY_TO_PICK/PICKING, v.v.
     */
    private boolean canCancel(Order order) {
        if (order == null) return false;

        // Dựa trên enum thay vì so sánh chuỗi
        OrderStauts os = order.getOrderStauts();
        if (os != null) {
            // Những trạng thái đơn nội bộ không được phép huỷ
            EnumSet<OrderStauts> nonCancelableOrder =
                    EnumSet.of(OrderStauts.SHIPPING, OrderStauts.SHIPPED, OrderStauts.COMPLETED, OrderStauts.CANCELLED);
            if (nonCancelableOrder.contains(os)) return false;
        }

        // GHN từ DELIVERING trở đi thì chặn huỷ
        GhnStatus gs = order.getGhnStatus();
        if (gs != null) {
            EnumSet<GhnStatus> nonCancelableGhn = EnumSet.of(GhnStatus.DELIVERING, GhnStatus.DELIVERED);
            if (nonCancelableGhn.contains(gs)) return false;
        }

        // Còn lại cho phép huỷ
        return true;
    }

    private static final Map<GhnStatus, Integer> GHN_RANK = Map.of(
            GhnStatus.READY_TO_PICK, 1,
            GhnStatus.PICKING,       2,
            GhnStatus.DELIVERING,    3,
            GhnStatus.DELIVERED,     4
    );

    private static String normalizeVi(String s) {
        if (s == null) return null;
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
        // Ð/ð -> Đ/đ (tránh ký tự lạ trong dữ liệu)
        s = s.replace('\u00D0', '\u0110').replace('\u00F0', '\u0111');
        return s.replaceAll("\\s+", " ").trim();
    }

    @Transactional
    @Override
    public void cancelOrderHasPayment(HttpSession session, Long orderId, String bankName, String bankReferenceCode) {
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) throw new IllegalStateException("Bạn cần đăng nhập trước.");

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + orderId));

        boolean isOwner = order.getUsers() != null && Objects.equals(order.getUsers().getId(), user.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(String.valueOf(user.getRole()));
        if (!isOwner && !isAdmin) throw new IllegalStateException("Bạn không có quyền huỷ đơn hàng này.");

        if (!canCancel(order)) {
            throw new IllegalStateException("Đơn hiện tại không thể huỷ (trạng thái: " + order.getOrderStauts() + ").");
        }
        String ghnCode = order.getGhnOrderCode();
        if (ghnCode != null && !ghnCode.isBlank()) {
            try {
                ghnClientService.cancelOrder(ghnCode);
                log.info("GHN cancel OK for order {}, code {}", orderId, ghnCode);
            } catch (WebClientResponseException wcre) {
                log.warn("Huỷ GHN thất bại: status={} body={}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
            } catch (Exception ex) {
                log.warn("Huỷ GHN thất bại: {}", ex.getMessage(), ex);
            }
        }

        // Cập nhật trạng thái nội bộ
        order.setOrderStauts(OrderStauts.CANCELLED);
        order.setGhnStatus(GhnStatus.CANCEL);
        order.setBankName(bankName);
        order.setBankReferenceCode(bankReferenceCode);
        orderRepo.save(order);
    }

    @Override
    public List<Order> getCancelledOrdersWithPayment() {
        return orderRepo.findCancelledOrdersWithPayButNoRefund();
    }
}
