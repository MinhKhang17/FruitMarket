package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.CreateOrderReq;
import com.example.fruitmarket.dto.CreateOrderRes;
import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.service.*;
import com.example.fruitmarket.util.AuthUtils;
import com.example.fruitmarket.util.UserUtil;
import com.example.fruitmarket.util.QrUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;
    private final GhnClientService ghnClientService;
    @Autowired private DistrictService districtService;
    @Autowired private ProvinceService provinceService;
    @Autowired private WardService wardService;

    // cấu hình trong application.yml/properties
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

    @Autowired
    private VnPayService vnPayService;
    @GetMapping
    public String viewCart(Model model,HttpSession session) {
        if(!UserUtil.isLogin(session)){
            return "redirect:/auth/login";
        }
        if (!AuthUtils.isClient(session)) {
            return "redirect:/";
        }
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "home/cart/view"; // tạo template cart/view.html
    }

    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "1") int qty,
            @RequestHeader(value = "Referer", required = false) String referer,
            HttpSession session,
            RedirectAttributes ra
    ) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.addToCart(productId, variantId, qty);
        // redirect về trang gọi thêm (referer) hoặc /cart
        return "redirect:" + (referer != null ? referer : "/cart");
    }

    @PostMapping("/update")
    public String updateQty(@RequestParam Long productId,
                            @RequestParam(required=false) Long variantId,
                            @RequestParam int qty,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.updateQuantity(productId, variantId, qty);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long productId,
                         @RequestParam(required=false) Long variantId,
                         HttpSession session,
                         RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.remove(productId, variantId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.clear();
        return "redirect:/cart";
    }
//    @PostMapping("/checkout")
//    public String checkoutCart(Model model, HttpSession session, RedirectAttributes ra) {
//        // 1. Kiểm tra login
//        if (session.getAttribute("loggedUser") == null) {
//            ra.addFlashAttribute("message", "You should login first");
//            ra.addFlashAttribute("type", "danger");
//            return "redirect:/auth/login";
//        }
//
//        // 2. Lấy giỏ hàng
//        Cart cart = cartService.getCart();
//        if (cart == null || cart.isEmpty()) {
//            ra.addFlashAttribute("message", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
//            ra.addFlashAttribute("type", "warning");
//            return "redirect:/cart";
//        }
//
//        // 3. Chuẩn bị dữ liệu cho view
//        model.addAttribute("cart", cart);
//        model.addAttribute("totalPrice", cart.getTotalPrice());
//        model.addAttribute("totalQuantity", cart.getTotalQuantity());
//
//        // 4. Lấy địa chỉ người dùng (bạn có service userService.getUserDetailFromSession)
//        List<User_detail> userDetails = userService.getUserDetailFromSession(session);
//        model.addAttribute("userDetail", userDetails);
//
//        // 5. Trả về template dành cho checkout toàn giỏ hàng
//        return "home/checkout-cart";
//    }
@PostMapping("/checkout")
public String checkoutCart(
        @RequestParam(name = "variantIds", required = false) List<Long> variantIds,
        @RequestParam(name = "quantities", required = false) List<Integer> quantities,
        Model model,
        HttpSession session,
        RedirectAttributes ra
) {
    // 1. Kiểm tra login
    if (session.getAttribute("loggedUser") == null) {
        ra.addFlashAttribute("message", "You should login first");
        ra.addFlashAttribute("type", "danger");
        return "redirect:/auth/login";
    }

    // 2. Lấy giỏ hàng
    Cart cart = cartService.getCart();
    if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
        ra.addFlashAttribute("message", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
        ra.addFlashAttribute("type", "warning");
        return "redirect:/cart";
    }

    // ===== QUAN TRỌNG: Kiểm tra variantIds thay vì productIds =====
    if (variantIds == null || variantIds.isEmpty()) {
        // Checkout toàn bộ giỏ
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", cart.getTotalPrice());
        model.addAttribute("totalQuantity", cart.getTotalQuantity());
        List<User_detail> userDetails = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail", userDetails);
        return "home/checkout-cart";
    }

    // 3. Build danh sách item được chọn theo variantId
    Map<Long, CartItem> cartIndex = new HashMap<>();
    for (CartItem ci : cart.getItems()) {
        if (ci.getVariantId() != null) {
            cartIndex.put(ci.getVariantId(), ci);
        }
    }

    log.info("Checkout selected items: variantIds={}, quantities={}", variantIds, quantities);

    List<CartItem> selected = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    int totalQty = 0;

    int n = variantIds.size();
    for (int i = 0; i < n; i++) {
        Long vId = variantIds.get(i);
        Integer q = (quantities != null && quantities.size() > i) ? quantities.get(i) : 1;

        CartItem base = cartIndex.get(vId);
        if (base == null) {
            log.warn("VariantId {} not found in cart", vId);
            continue;
        }

        // Clone item với quantity được chọn
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

    // 4. Đưa dữ liệu ra view
    model.addAttribute("selectedItems", selected);
    model.addAttribute("totalPrice", total);
    model.addAttribute("totalQuantity", totalQty);

    List<User_detail> userDetails = userService.getUserDetailFromSession(session);
    model.addAttribute("userDetail", userDetails);

    return "home/checkout-cart";
}
    // helper key builder: productId#variantId (variantId may be null)
    private String buildKey(Long productId, Long variantId) {
        return (productId == null ? "null" : productId.toString()) + "#" + (variantId == null ? "null" : variantId.toString());
    }
    /* ======================
      --- NEW: PROCESS FINAL CHECKOUT (từ form checkout-cart.html)
      Endpoint: POST /checkout/process-cart
      Lưu ý: form của bạn gửi tới /checkout/process-cart (theo template bạn đưa).
      Mình thêm method này vào CartController để gom logic ở cùng 1 chỗ.
      ====================== */
    @PostMapping(path = "/process-checkout-from-page", consumes = {"application/x-www-form-urlencoded"})
    public String processCartCheckoutFromCartPage(
            @RequestParam(name = "addressId", required = false) Long addressId,
            @RequestParam(name = "paymentMethod", defaultValue = "COD") String paymentMethod,
            @RequestParam(name = "toDistrictId", required = false) Integer toDistrictId,
            @RequestParam(name = "toWardCode", required = false) String toWardCode,

            HttpSession session,
            RedirectAttributes ra,
            Model model,
            HttpServletRequest request
    ) {
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

        // 4) Build OrderRequest
        OrderRequest orderReq = new OrderRequest();
        orderReq.setAddressId(addressId);
        orderReq.setPaymentMethod(paymentMethod);
        orderReq.setToDistrictId(toDistrictId);
        orderReq.setToWardCode(toWardCode);

        List<OrderRequest.OrderItem> items = new ArrayList<>();
        cart.getItems().forEach(ci -> {
            OrderRequest.OrderItem oi = new OrderRequest.OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setVariantId(ci.getVariantId());
            oi.setName(ci.getName());
            oi.setPrice(ci.getPrice());
            oi.setQuantity(ci.getQuantity());
            items.add(oi);
        });
        orderReq.setItems(items);
        orderReq.setTotalPrice(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO);
        orderReq.setTotalQuantity(cart.getTotalQuantity());

        try {
            // 5) Tạo order nội bộ
            Long orderId = orderService.createOrderFromCart(orderReq, session);
            if (orderId == null || orderId <= 0L) {
                throw new IllegalStateException("Tạo đơn thất bại (invalid orderId). Vui lòng thử lại.");
            }

            Order order = orderService.getOrderById(orderId);

            // 6) Nếu VNPAY: trả trang QR
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                try {
                    String orderInfo = "Thanh toan don hang #" + order.getId();
                    String paymentUrl = vnPayService.createPaymentUrl(request, order.getTotalPrice(), orderInfo, order.getId());
                    String qrBase64 = QrUtils.generateQrBase64(paymentUrl);

                    model.addAttribute("paymentUrl", paymentUrl);
                    model.addAttribute("qrBase64", qrBase64);
                    model.addAttribute("order", order);
                    model.addAttribute("amount", order.getTotalPrice());
                    cartService.clear();
                    return "home/payment_qr";
                } catch (Exception e) {
                    e.printStackTrace();
                    ra.addFlashAttribute("message", "Lỗi tạo QR thanh toán VNPAY: " + e.getMessage());
                    ra.addFlashAttribute("type", "danger");
                    return "redirect:/";
                }
            }

            log.info("[processCartCheckout] addrId={}, pay={}, toDistrictId={}, toWardCode='{}'",
                    addressId, paymentMethod, toDistrictId, toWardCode);

            boolean ghnCond = (toDistrictId != null && toDistrictId > 0 && toWardCode != null && !toWardCode.isBlank());
            log.info("[processCartCheckout] GHN condition = {}", ghnCond);

            if (ghnCond) {
                // Cập nhật geo cho địa chỉ đã chọn
                userService.updateAddressGeo(addressId, toDistrictId, toWardCode);

                // 1) Lấy service_id
                var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
                if (svRes == null || svRes.getData() == null || svRes.getData().isEmpty())
                    throw new IllegalStateException("Không lấy được service_id từ GHN. Kiểm tra fromDistrictId/toDistrictId.");
                Integer serviceId = svRes.getData().get(0).getServiceId();

                // 2) Tính tổng cân nặng của đơn (đơn giản: dùng defaultWeight nếu bạn coi là tổng)
                // Bạn có thể cải tiến: lấy weight theo từng item/variant nếu có.
                long totalWeight = Math.max(100, defaultWeight); // gram, tối thiểu 100g

                // 3) TÍNH PHÍ SHIP GHN (signature theo code bạn đang gọi)
                var feeRes = ghnClientService.calculateFee(
                        fromDistrictId,                  // ✅ from_district_id
                        toDistrictId,                    // ✅ to_district_id
                        toWardCode,                      // ✅ to_ward_code
                        serviceId,                       // ✅ service_id
                        totalWeight,                     // ✅ weight (gram)
                        defaultLength,                   // ✅ length (cm)
                        defaultWidth,                    // ✅ width (cm)
                        defaultHeight,                   // ✅ height (cm)
                        order.getTotalPrice().intValue() // ✅ insurance_value (VND)
                );
                if (feeRes == null || feeRes.getData() == null)
                    throw new IllegalStateException("Không tính được phí vận chuyển GHN.");

                int shippingFee = feeRes.getData().getTotal(); // đổi field cho đúng DTO của bạn

                // 4) Build danh sách items GHN (ít nhất có name & quantity)
                List<CreateOrderReq.Item> itemsGHN = order.getOrderItemList().stream()
                        .map(oi -> {
                            CreateOrderReq.Item it = new CreateOrderReq.Item();
                            it.setName(oi.getProductVariant().getProduct().getProductName());
                            it.setQuantity(oi.getQuanity());
                            return it;
                        })
                        .toList();

                if (itemsGHN.isEmpty()) {
                    throw new IllegalStateException("GHN yêu cầu ít nhất 1 item trong đơn.");
                }

                // (Tuỳ bạn) lưu phí ship vào order
                try {
                    orderService.updateShippingFee(orderId, BigDecimal.valueOf(shippingFee));
                } catch (Exception ignore) {
                    log.warn("updateShippingFee not implemented or failed, ignore.");
                }

                // 5) Tính COD theo người trả phí
                // paymentTypeId: 1 shop trả, 2 người nhận trả
                int paymentTypeId = 1; // hoặc lấy từ config/UI
                int shippingFees = feeRes.getData().getTotal();

                // Nếu muốn cộng phí ship vào tổng phải thanh toán của KH
                boolean chargeToCustomer = ("COD".equalsIgnoreCase(paymentMethod) && paymentTypeId == 1)
                        || "VNPAY".equalsIgnoreCase(paymentMethod);

                if (chargeToCustomer && shippingFees > 0) {
                    orderService.addShippingToTotal(orderId, BigDecimal.valueOf(shippingFees));
                }

                // Lấy lại order để có totalPrice mới khi cần dùng tiếp
                order = orderService.getOrderById(orderId);

                // Tính COD gửi cho GHN
                int codAmount = 0;
                if ("COD".equalsIgnoreCase(paymentMethod)) {
                    codAmount = order.getTotalPrice().intValue(); // đã cộng ship nếu chargeToCustomer = true
                }

                // 6) Gọi tạo đơn GHN
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
                if (coRes == null || coRes.getData() == null || coRes.getData().getOrderCode() == null)
                    throw new IllegalStateException("GHN không trả order_code: " + (coRes != null ? coRes.getMessage() : "null"));

                orderService.attachShippingCode(orderId, coRes.getData().getOrderCode());
            }

            // 8) Xoá giỏ & điều hướng
            cartService.clear();
            ra.addFlashAttribute("message", "Đặt hàng thành công! Mã đơn hàng: " + orderId);
            ra.addFlashAttribute("type", "success");
            return "redirect:/myOrders/" + orderId;

        } catch (Exception ex) {
            ra.addFlashAttribute("message", "Có lỗi khi tạo đơn hàng: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
            ra.addFlashAttribute("type", "danger");

            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);

            return "home/checkout-cart";
        }
    }

    /**
     * Helper safe logger: trả về id hoặc chuỗi an toàn thay vì in toàn object Users
     */
    private String safeUserId(Object logged) {
        try {
            if (logged == null) return "null";
            if (logged instanceof com.example.fruitmarket.model.Users) {
                Integer id = ((com.example.fruitmarket.model.Users) logged).getId();
                return id != null ? String.valueOf(id) : "unknown-id";
            }
            return String.valueOf(logged);
        } catch (Exception e) {
            return "unknown";
        }
    }

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

        return "redirect:cart/checkout";
    }

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

}
