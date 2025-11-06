package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.CreateOrderReq;
import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.service.*;
import com.example.fruitmarket.util.AuthUtils;
import com.example.fruitmarket.util.QrUtils;
import com.example.fruitmarket.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    // ===== Services qua constructor (final) =====
    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;
    private final GhnClientService ghnClientService;
    private final DistrictService districtService;
    private final ProvinceService provinceService;
    private final WardService wardService;

    // Nếu bạn đang dùng VnPayService theo field injection, giữ lại @Autowired
    @Autowired
    private VnPayService vnPayService;

    // ===== Cấu hình GHN =====
    @Value("${ghn.from-district-id:0}")
    private int fromDistrictId;
    @Value("${ghn.default.weight:500}")
    private int defaultWeight;
    @Value("${ghn.default.length:20}")
    private int defaultLength;
    @Value("${ghn.default.width:15}")
    private int defaultWidth;
    @Value("${ghn.default.height:8}")
    private int defaultHeight;

    /* ======================
     * GET /cart: xem giỏ
     * ====================== */
    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        if (!UserUtil.isLogin(session)) {
            return "redirect:/auth/login";
        }
        if (!AuthUtils.isClient(session)) {
            return "redirect:/";
        }
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "home/cart/view";
    }

    /* ======================
     * POST /cart/add: thêm vào giỏ
     * ====================== */
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(required = false) Long variantId,
                            @RequestParam(defaultValue = "1") int qty,
                            @RequestHeader(value = "Referer", required = false) String referer,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.addToCart(productId, variantId, qty);
        return "redirect:" + (referer != null ? referer : "/cart");
    }

    /* ======================
     * POST /cart/update: cập nhật số lượng
     * ====================== */
    @PostMapping("/update")
    public String updateQty(@RequestParam Long productId,
                            @RequestParam(required = false) Long variantId,
                            @RequestParam int qty,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.updateQuantity(productId, variantId, qty);
        return "redirect:/cart";
    }

    /* ======================
     * POST /cart/remove: xóa 1 item
     * ====================== */
    @PostMapping("/remove")
    public String remove(@RequestParam Long productId,
                         @RequestParam(required = false) Long variantId,
                         HttpSession session,
                         RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.remove(productId, variantId);
        return "redirect:/cart";
    }

    /* ======================
     * POST /cart/clear: xóa toàn giỏ
     * ====================== */
    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.clear();
        return "redirect:/cart";
    }

    /* =======================================================================================
     * POST /cart/checkout
     * Màn hình checkout:
     * - Nếu KHÔNG truyền variantIds => checkout toàn bộ giỏ
     * - Nếu CÓ truyền variantIds kèm quantities => checkout các biến thể được chọn
     * Trả về view: home/checkout-cart (chỉ hiển thị thông tin, CHƯA tạo đơn)
     * ======================================================================================= */
    @PostMapping("/checkout")
    public String checkoutCart(@RequestParam(name = "variantIds", required = false) List<Long> variantIds,
                               @RequestParam(name = "quantities", required = false) List<Integer> quantities,
                               Model model,
                               HttpSession session,
                               RedirectAttributes ra) {
        // 1) Kiểm tra login
        if (session.getAttribute("loggedUser") == null) {
            ra.addFlashAttribute("message", "You should login first");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        // 2) Lấy giỏ
        Cart cart = cartService.getCart();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            ra.addFlashAttribute("message", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 3) Nếu không chọn item cụ thể => checkout toàn giỏ
        if (variantIds == null || variantIds.isEmpty()) {
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);
            return "home/checkout-cart";
        }

        // 4) Build map variantId -> CartItem trong giỏ
        Map<Long, CartItem> byVariant = new HashMap<>();
        for (CartItem ci : cart.getItems()) {
            if (ci.getVariantId() != null) {
                byVariant.put(ci.getVariantId(), ci);
            }
        }

        log.info("Checkout selected items: variantIds={}, quantities={}", variantIds, quantities);

        List<CartItem> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int totalQty = 0;

        for (int i = 0; i < variantIds.size(); i++) {
            Long vId = variantIds.get(i);
            Integer q = (quantities != null && quantities.size() > i) ? quantities.get(i) : 1;

            CartItem base = byVariant.get(vId);
            if (base == null) {
                log.warn("VariantId {} not found in cart", vId);
                continue;
            }

            CartItem sel = new CartItem();
            sel.setProductId(base.getProductId());
            sel.setVariantId(base.getVariantId());
            sel.setName(base.getName());
            sel.setVariantName(base.getVariantName());
            sel.setPrice(base.getPrice());
            sel.setImageUrl(base.getImageUrl());
            sel.setQuantity((q == null || q < 1) ? 1 : q);

            BigDecimal sub = (sel.getPrice() == null) ? BigDecimal.ZERO
                    : sel.getPrice().multiply(BigDecimal.valueOf(sel.getQuantity()));

            selected.add(sel);
            total = total.add(sub);
            totalQty += sel.getQuantity();
        }

        if (selected.isEmpty()) {
            ra.addFlashAttribute("message", "Không có mục hợp lệ để thanh toán. Vui lòng kiểm tra lại lựa chọn.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        log.info("Selected {} items for checkout, total: {}", selected.size(), total);

        // 5) Đưa dữ liệu ra view
        model.addAttribute("selectedItems", selected);
        model.addAttribute("totalPrice", total);
        model.addAttribute("totalQuantity", totalQty);
        List<User_detail> userDetails = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail", userDetails);

        return "home/checkout-cart";
    }

    /* ======================================================================
     * POST /cart/process-checkout-from-page
     * Nhận form từ trang checkout-cart.html để TẠO ĐƠN + GHN + VNPAY
     * ====================================================================== */
    @PostMapping(path = "/process-checkout-from-page", consumes = {"application/x-www-form-urlencoded"})
    public String processCartCheckoutFromCartPage(@RequestParam(name = "addressId", required = false) Long addressId,
                                                  @RequestParam(name = "paymentMethod", defaultValue = "COD") String paymentMethod,
                                                  @RequestParam(name = "toDistrictId", required = false) Integer toDistrictId,
                                                  @RequestParam(name = "toWardCode", required = false) String toWardCode,
                                                  HttpSession session,
                                                  RedirectAttributes ra,
                                                  Model model,
                                                  HttpServletRequest request) {
        // 1) Login check
        Object logged = session.getAttribute("loggedUser");
        if (logged == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để tiếp tục thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        // 2) Lấy giỏ
        Cart cart = cartService.getCart();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            ra.addFlashAttribute("message", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 3) Kiểm tra địa chỉ
        if (addressId == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn địa chỉ giao hàng.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 4) Build OrderRequest từ giỏ
        OrderRequest orderReq = new OrderRequest();
        orderReq.setAddressId(addressId);
        orderReq.setPaymentMethod(paymentMethod);
        orderReq.setToDistrictId(toDistrictId);
        orderReq.setToWardCode(toWardCode);

        List<OrderRequest.OrderItem> items = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            OrderRequest.OrderItem oi = new OrderRequest.OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setVariantId(ci.getVariantId());
            oi.setName(ci.getName());
            oi.setPrice(ci.getPrice());
            oi.setQuantity(ci.getQuantity());
            items.add(oi);
        }
        orderReq.setItems(items);
        orderReq.setTotalPrice(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO);
        orderReq.setTotalQuantity(cart.getTotalQuantity());

        try {
            // 5) Tạo Order nội bộ
            Long orderId = orderService.createOrderFromCart(orderReq, session);
            if (orderId == null || orderId <= 0L) {
                throw new IllegalStateException("Tạo đơn thất bại (invalid orderId). Vui lòng thử lại.");
            }

            Order order = orderService.getOrderById(orderId);

            // 6) Nếu VNPAY: render trang QR
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                String orderInfo = "Thanh toan don hang #" + order.getId();
                String paymentUrl = vnPayService.createPaymentUrl(request, order.getTotalPrice(), orderInfo, order.getId());
                String qrBase64 = QrUtils.generateQrBase64(paymentUrl);

                model.addAttribute("paymentUrl", paymentUrl);
                model.addAttribute("qrBase64", qrBase64);
                model.addAttribute("order", order);
                model.addAttribute("amount", order.getTotalPrice());

                cartService.clear();
                return "home/payment_qr";
            }

            // 7) Nếu có geo => GHN
            log.info("[processCartCheckout] addrId={}, pay={}, toDistrictId={}, toWardCode='{}'",
                    addressId, paymentMethod, toDistrictId, toWardCode);

            boolean ghnCond = (toDistrictId != null && toDistrictId > 0 && toWardCode != null && !toWardCode.isBlank());
            log.info("[processCartCheckout] GHN condition = {}", ghnCond);

            if (ghnCond) {
                // Cập nhật geo cho địa chỉ đã chọn
                userService.updateAddressGeo(addressId, toDistrictId, toWardCode);

                // 1) service_id
                var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
                if (svRes == null || svRes.getData() == null || svRes.getData().isEmpty()) {
                    throw new IllegalStateException("Không lấy được service_id từ GHN. Kiểm tra fromDistrictId/toDistrictId.");
                }
                Integer serviceId = svRes.getData().get(0).getServiceId();

                // 2) cân nặng tổng
                long totalWeight = Math.max(100, defaultWeight); // gram

                // 3) tính phí
                var feeRes = ghnClientService.calculateFee(
                        fromDistrictId,
                        toDistrictId,
                        toWardCode,
                        serviceId,
                        totalWeight,
                        defaultLength,
                        defaultWidth,
                        defaultHeight,
                        order.getTotalPrice().intValue()
                );
                if (feeRes == null || feeRes.getData() == null) {
                    throw new IllegalStateException("Không tính được phí vận chuyển GHN.");
                }
                int shippingFee = feeRes.getData().getTotal();

                // 4) items cho GHN
                List<CreateOrderReq.Item> itemsGHN = order.getOrderItemList().stream()
                        .map(oi -> {
                            CreateOrderReq.Item it = new CreateOrderReq.Item();
                            it.setName(oi.getProductVariant().getProduct().getProductName());
                            it.setQuantity(oi.getQuanity());
                            return it;
                        })
                        .collect(Collectors.toList());

                if (itemsGHN.isEmpty()) {
                    throw new IllegalStateException("GHN yêu cầu ít nhất 1 item trong đơn.");
                }

                // (tuỳ bạn) lưu phí ship vào order
                try {
                    orderService.updateShippingFee(orderId, BigDecimal.valueOf(shippingFee));
                } catch (Exception e) {
                    log.warn("updateShippingFee not implemented or failed, ignore.");
                }

                // 5) người trả phí
                int paymentTypeId = 1; // 1 shop trả, 2 người nhận trả (tuỳ UI/config)
                int shippingFees = feeRes.getData().getTotal();

                boolean chargeToCustomer = ("COD".equalsIgnoreCase(paymentMethod) && paymentTypeId == 1)
                        || "VNPAY".equalsIgnoreCase(paymentMethod);
                if (chargeToCustomer && shippingFees > 0) {
                    orderService.addShippingToTotal(orderId, BigDecimal.valueOf(shippingFees));
                }

                // Lấy lại order (có thể đã cộng ship)
                order = orderService.getOrderById(orderId);

                // COD gửi GHN
                int codAmount = 0;
                if ("COD".equalsIgnoreCase(paymentMethod)) {
                    codAmount = order.getTotalPrice().intValue();
                }

                // 6) Tạo đơn GHN
                CreateOrderReq co = new CreateOrderReq();
                co.setToName(order.getUsers() != null ? order.getUsers().getUsername() : "Khách hàng");
                co.setToPhone(order.getPhoneNumber());
                co.setToAddress(order.getAddress());
                co.setToWardCode(toWardCode);
                co.setToDistrictId(toDistrictId);
                co.setServiceId(serviceId);
                co.setWeight(totalWeight);
                co.setLength(defaultLength);
                co.setWidth(defaultWidth);
                co.setHeight(defaultHeight);
                co.setCodAmount(codAmount);
                co.setClientOrderCode("ORD-" + orderId);
                co.setNote("Don hang tu he thong FruitMarket");
                co.setItems(itemsGHN);
                co.setPayment_type_id(paymentTypeId);
                co.setRequired_note("KHONGCHOXEMHANG");

                var coRes = ghnClientService.createOrder(co);
                if (coRes == null || coRes.getData() == null || coRes.getData().getOrderCode() == null) {
                    throw new IllegalStateException("GHN không trả order_code: " + (coRes != null ? coRes.getMessage() : "null"));
                }

                orderService.attachShippingCode(orderId, coRes.getData().getOrderCode());
            }

            // 8) Clear giỏ & điều hướng
            cartService.clear();
            ra.addFlashAttribute("message", "Đặt hàng thành công! Mã đơn hàng: " + orderId);
            ra.addFlashAttribute("type", "success");
            return "redirect:/myOrders/" + orderId;

        } catch (Exception ex) {
            ra.addFlashAttribute("message", "Có lỗi khi tạo đơn hàng: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
            ra.addFlashAttribute("type", "danger");

            // render lại trang checkout-cart với dữ liệu hiện có
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);

            return "home/checkout-cart";
        }
    }

    /* ======================
     * Lưu địa chỉ từ popup ở trang checkout
     * ====================== */
    @PostMapping("/checkout/save-address")
    public String saveCartAddress(@RequestParam String phone,
                                  @RequestParam String address,
                                  @RequestParam(required = false) Integer provinceId,
                                  @RequestParam(required = false) Integer districtId,
                                  @RequestParam(required = false) String wardCode,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        Users loggedUser = (Users) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            ra.addFlashAttribute("message", "Please login first");
            return "redirect:/auth/login";
        }

        User_detail detail = new User_detail();
        detail.setPhone(phone);
        detail.setAddress(address);
        detail.setUser(loggedUser);

        if (provinceId != null) {
            Province province = provinceService.findByProvinceId(provinceId);
            detail.setProvince(province);
        }
        if (districtId != null) {
            District district = districtService.findByDistrictId(districtId);
            detail.setDistrict(district);
        }
        if (wardCode != null && !wardCode.isBlank()) {
            Ward ward = wardService.findByWardCode(wardCode);
            detail.setWard(ward);
        }

        userService.saveUserDetail(detail);

        ra.addFlashAttribute("message", "✅ Đã thêm địa chỉ giao hàng mới!");
        ra.addFlashAttribute("type", "success");

        // quay lại giỏ/checkout để tiếp tục thao tác
        return "redirect:/cart";
    }

    /* ======================
     * Helpers
     * ====================== */
    private boolean denyIfNotClient(HttpSession session, RedirectAttributes ra) {
        if (!UserUtil.isLogin(session)) {
            ra.addFlashAttribute("message", "Vui lòng đăng nhập để sử dụng giỏ hàng.");
            ra.addFlashAttribute("type", "warning");
            return true;
        }
        if (!AuthUtils.isClient(session)) {
            ra.addFlashAttribute("message", "Chỉ khách hàng mới có thể thao tác trên giỏ hàng.");
            ra.addFlashAttribute("type", "danger");
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    private String safeUserId(Object logged) {
        try {
            if (logged == null) return "null";
            if (logged instanceof Users) {
                Integer id = ((Users) logged).getId();
                return id != null ? String.valueOf(id) : "unknown-id";
            }
            return String.valueOf(logged);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
