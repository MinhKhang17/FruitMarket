package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.CreateOrderReq;
import com.example.fruitmarket.dto.CreateOrderRes;
import com.example.fruitmarket.dto.OrderDetailRes;
import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.enums.OrderStauts;
import com.example.fruitmarket.enums.PricingMethod;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.OrderRepo;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired UserService userService;
    @Autowired OrderRepo orderRepo;
    @Autowired ProductService productService;
    @Autowired GhnClientService ghnClientService;

    @Value("${ghn.from-district-id}")
    private int fromDistrictId;

    @Override
    @Transactional  // ← thêm để đảm bảo cùng 1 TX
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
        oi.setProductVariant(variant); // dùng luôn object đang có
        oi.setPrice(variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO);

        order.setOrderItemList(new ArrayList<>());
        order.getOrderItemList().add(oi);

        // totals
        BigDecimal goodsTotal = oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuanity()));
        BigDecimal safeShip = (shippingFee != null && shippingFee.signum() >= 0) ? shippingFee : BigDecimal.ZERO;
        order.setTotalPrice(goodsTotal.add(safeShip));
        order.setTotalQuantity(oi.getQuanity());
        order.setShippingFee(safeShip);

        Order saved = orderRepo.save(order);

        // === GHN create order (best-effort) ===
        try {
            var svRes = ghnClientService.availableServices(fromDistrictId, ud.getDistrict().getDistrictId());
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int useServiceId = (serviceId != null && serviceId > 0)
                        ? serviceId
                        : svRes.getData().get(0).getServiceId();

                long weight = Math.max(1, saved.getTotalQuantity()) * 500;
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

                CreateOrderRes ghRes = ghnClientService.createOrder(req);
                if (ghRes != null && ghRes.getData() != null && ghRes.getData().getOrderCode() != null) {
                    saved.setGhnOrderCode(ghRes.getData().getOrderCode());
                    OrderDetailRes detail = ghnClientService.getOrderDetail(saved.getGhnOrderCode());
                    if (detail != null && detail.getData() != null) {
                        saved.setGhnStatus(detail.getData().getStatus());
                    }
                    orderRepo.saveAndFlush(saved); // flush ngay
                } else {
                    System.out.println("[GHN] createOrder: no orderCode returned");
                }
            } else {
                System.out.println("[GHN] no available service for route " + fromDistrictId + " -> " + ud.getDistrict().getDistrictId());
            }
        } catch (Exception ex) {
            ex.printStackTrace(); // đừng nuốt lỗi
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
            // nếu entity OrderItem có quan hệ @ManyToOne Order, set ngược lại:
            // oi.setOrder(order);

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
                // dùng DTO getter camelCase
                int serviceId = svRes.getData().get(0).getServiceId();

                // có weight trên variant thì cộng dồn; không thì mặc định 500g/item
                int weight = Math.max(1, totalQty) * 500;
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
        }

        order.setShippingFee(shippingFee);
        order.setTotalPrice(goodsTotal.add(shippingFee)); // ✅ tổng = hàng + ship

        // ===== 3) LƯU ĐƠN =====
        Order saved = orderRepo.save(order);

        // ===== 4) (tuỳ chọn) tạo đơn GHN sau khi lưu thành công =====
        try {
            String toName = user.getUsername();
            String toPhone = userDetail.getPhone();
            String toAddr  = userDetail.getAddress();
            String toWardCode   = userDetail.getWard().getWardCode();     // đảm bảo user_detail có ward/district
            Integer toDistrictId= userDetail.getDistrict().getDistrictId();

            var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
            if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                int serviceId = svRes.getData().get(0).getServiceId();

                long weight = Math.max(1, order.getTotalQuantity()) * 500; // ví dụ
                int length = 20, width = 15, height = 10;

                int cod = order.getPricingMethod() == PricingMethod.COD
                        ? order.getTotalPrice().intValue()   // nếu COD gửi tổng tiền
                        : 0;                                  // VNPAY thì 0

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

                CreateOrderRes ghRes = ghnClientService.createOrder(req);
                if (ghRes != null && ghRes.getData() != null && ghRes.getData().getOrderCode() != null) {
                    saved.setGhnOrderCode(ghRes.getData().getOrderCode());
                    // (tuỳ chọn) lấy status
                    var detail = ghnClientService.getOrderDetail(saved.getGhnOrderCode());
                    if (detail != null && detail.getData() != null) {
                        saved.setGhnStatus(detail.getData().getStatus());
                    }
                    orderRepo.save(saved); // hoặc saveAndFlush
                }
            }
        } catch (Exception ex) {
            // log.warn("Create GHN failed", ex);
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
    public void updateFromGhnCallback(String clientOrderCode, String ghnOrderCode, String ghnStatus, Integer codAmount) {
        Long orderId = parseOrderIdFromClientOrderCode(clientOrderCode);

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng từ clientOrderCode: " + clientOrderCode));

        if (order.getGhnOrderCode() == null || order.getGhnOrderCode().isBlank()) {
            order.setGhnOrderCode(ghnOrderCode);
        }

        switch (safe(ghnStatus)) {
            case "ready_to_pick":
            case "picking":
            case "picked":
            case "storing":
            case "transporting":
            case "delivering":
                order.setOrderStauts(OrderStauts.SHIPPING);
                break;
            case "delivered":
                order.setOrderStauts(OrderStauts.SHIPPED);
                break;
            case "cancel":
            case "returned":
            case "return":
                order.setOrderStauts(OrderStauts.CANCELLED);
                break;
            default:
                order.setOrderStauts(OrderStauts.PENDING);
                break;
        }

        order.setGhnStatus(ghnStatus);
        orderRepo.save(order);
    }

    // ===== helper =====
    private String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private Long parseOrderIdFromClientOrderCode(String code) {
        if (code == null) throw new IllegalArgumentException("clientOrderCode null");
        String c = code.trim();
        if (c.startsWith("ORD-")) {
            try { return Long.parseLong(c.substring(4)); }
            catch (NumberFormatException ignored) {}
        }
        try { return Long.parseLong(c); }
        catch (NumberFormatException e) {
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
