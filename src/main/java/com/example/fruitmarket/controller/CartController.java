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

/**
 * Controller quản lý giỏ hàng (đã merge):
 * - Xem / Thêm / Cập nhật / Xoá / Xoá toàn bộ
 * - Checkout toàn giỏ hoặc theo các biến thể được chọn
 * - Tạo đơn: COD hoặc VNPAY, tích hợp GHN (availableServices, calculateFee, createOrder)
 * - Lưu địa chỉ giao hàng
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    // Services qua constructor
    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;
    private final GhnClientService ghnClientService;
    private final DistrictService districtService;
    private final ProvinceService provinceService;
    private final WardService wardService;
private final ProductService productService;
    // VNPAY (giữ field injection theo code của bạn)
    @Autowired
    private VnPayService vnPayService;

    // Cấu hình GHN
    @Value("${ghn.from-district-id}")
    private int fromDistrictId;
    @Value("${ghn.from-ward-code}")
    private String fromWardCode;
    @Value("${ghn.default.weight:500}")
    private int defaultWeight;
    @Value("${ghn.default.length:20}")
    private int defaultLength;
    @Value("${ghn.default.width:15}")
    private int defaultWidth;
    @Value("${ghn.default.height:8}")
    private int defaultHeight;

    // ======================
    // 📦 HIỂN THỊ GIỎ HÀNG
    // ======================
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

    // ======================
    // ➕ THÊM SẢN PHẨM VÀO GIỎ (hỗ trợ số lượng hoặc khối lượng)
    // ======================
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(required = false) Long variantId,
                            @RequestParam(name = "qtyOrWeight", required = false) Double qtyOrWeight,
                            @RequestParam(name = "quantity",    required = false) Double quantity,
                            @RequestParam(name = "weight",      required = false) Double weight,
                            @RequestHeader(value = "Referer", required = false) String referer,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";

        // Ưu tiên weight > quantity > qtyOrWeight, default 1.0
        double val = (weight != null ? weight : (quantity != null ? quantity : (qtyOrWeight != null ? qtyOrWeight : 1.0)));
        log.info("🛒 addToCart: productId={}, variantId={}, val={}", productId, variantId, val);
        ra.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng thành công!");
        // CartService của bạn cần hỗ trợ double (đã có ở controller cũ). Nếu chỉ có int, đổi Math.floor(val).
        cartService.addToCart(productId, variantId, val);
        return "redirect:" + (referer != null ? referer : "/cart");
    }

    // ======================
    // 🔄 CẬP NHẬT SỐ LƯỢNG / KHỐI LƯỢNG
    // ======================
    @PostMapping("/update")
    public String updateQty(@RequestParam Long productId,
                            @RequestParam(required = false) Long variantId,
                            @RequestParam(name = "qtyOrWeight") double qtyOrWeight,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        log.info("♻️ updateCart: productId={}, variantId={}, qtyOrWeight={}", productId, variantId, qtyOrWeight);
        cartService.updateQuantity(productId, variantId, qtyOrWeight);
        return "redirect:/cart";
    }

    // ======================
    // 🗑️ XOÁ ITEM
    // ======================
    @PostMapping("/remove")
    public String remove(@RequestParam Long productId,
                         @RequestParam(required = false) Long variantId,
                         HttpSession session,
                         RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.remove(productId, variantId);
        return "redirect:/cart";
    }

    // ======================
    // ❌ XOÁ TOÀN BỘ GIỎ
    // ======================
    @PostMapping("/clear")
    public String clear(HttpSession session, RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/";
        cartService.clear();
        return "redirect:/cart";
    }

    /* =======================================================================================
     * POST /cart/checkout
     * - Không truyền variantIds => checkout toàn giỏ
     * - Có variantIds + quantities (Double) => checkout các biến thể được chọn
     * Trả view: home/checkout-cart (chỉ hiển thị, CHƯA tạo đơn)
     * ======================================================================================= */
    @PostMapping("/checkout")
    public String checkoutCart(@RequestParam(name = "variantIds", required = false) List<Long> variantIds,
                               @RequestParam(name = "quantities", required = false) List<Double> quantities,
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

        // 3) Không chọn item -> toàn giỏ
        if (variantIds == null || variantIds.isEmpty()) {
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            model.addAttribute("totalWeight", cartService.getTotalWeight());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);
            return "home/checkout-cart";
        }

        // 4) Map variantId -> CartItem
        Map<Long, CartItem> cartIndex = new HashMap<>();
        for (CartItem ci : cart.getItems()) {
            if (ci.getVariantId() != null) cartIndex.put(ci.getVariantId(), ci);
        }

        List<CartItem> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int totalQty = 0;

        int n = variantIds.size();
        for (int i = 0; i < n; i++) {
            Long vId = variantIds.get(i);
            Double val = (quantities != null && quantities.size() > i) ? quantities.get(i) : 1.0;

            CartItem base = cartIndex.get(vId);
            if (base == null) continue;

            CartItem sel = new CartItem();
            sel.setProductId(base.getProductId());
            sel.setVariantId(base.getVariantId());
            sel.setName(base.getName());
            sel.setVariantName(base.getVariantName());
            sel.setPrice(base.getPrice());
            sel.setImageUrl(base.getImageUrl());
            sel.setUnit(base.getUnit());

            if ("KILOGRAM".equalsIgnoreCase(base.getUnit())) {
                sel.setWeight(Math.max(0.1, val)); // tối thiểu 0.1kg
            } else {
                sel.setQuantity((int) Math.max(1, Math.floor(val)));
            }

            BigDecimal sub = sel.getSubTotal(); // dựa vào model CartItem của bạn
            selected.add(sel);
            total = total.add(sub);
            totalQty += ("KILOGRAM".equalsIgnoreCase(sel.getUnit())) ? 0 : sel.getQuantity();
        }

        if (selected.isEmpty()) {
            ra.addFlashAttribute("message", "Không có mục hợp lệ để thanh toán.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        model.addAttribute("selectedItems", selected);
        model.addAttribute("totalPrice", total);
        model.addAttribute("totalQuantity", totalQty);

        List<User_detail> userDetails = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail", userDetails);

        return "home/checkout-cart";
    }

    /* ======================================================================
     * POST /cart/process-checkout-from-page
     * Nhận form từ checkout-cart.html để TẠO ĐƠN + GHN + VNPAY
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

            if ("KILOGRAM".equalsIgnoreCase(ci.getUnit())) {
                oi.setWeight(ci.getWeight()); // dùng weight khi bán theo kg
            } else {
                oi.setQuantity(ci.getQuantity()); // dùng quantity khi bán theo cái
            }
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

            // ====== NEW: cập nhật stock cho từng variant dựa trên orderReq.items ======
            // Giả sử bạn có ProductService với findProductVariantById & saveProductVariant
            try {
                for (OrderRequest.OrderItem oi : orderReq.getItems()) {
                    Long variantId = oi.getVariantId();
                    if (variantId == null) continue;

                    try {
                        ProductVariant pv = productService.findProductVariantById(variantId);
                        if (pv == null) {
                            log.warn("Không tìm thấy ProductVariant id={} để cập nhật stock (order {})", variantId, orderId);
                            continue;
                        }

                        Long currentStock = pv.getStock(); // giả sử trường là Integer stock
                        if (currentStock == null) {
                            log.warn("ProductVariant id={} không có trường stock (order {}) — bỏ qua cập nhật", variantId, orderId);
                            continue;
                        }

                        int deduct = 0;
                        if (oi.getQuantity() != null) {
                            deduct = oi.getQuantity();
                        } else if (oi.getWeight() != null) {
                            // Nếu bạn lưu stock theo "cái", và bán theo kg, cần quy đổi.
                            // Ở đây tạm tính giảm = ceil(weight) — điều chỉnh theo thực tế của bạn.
                            deduct = (int) Math.ceil(oi.getWeight());
                        }

                        Long newStock =  currentStock - deduct;
                        pv.setStock(newStock);

                        // Lưu thay đổi (method tên có thể khác trong project của bạn)
                        productService.saveProductVariant(pv);

                        log.info("Cập nhật stock variantId={} : {} -> {} (deduct={}), order={}",
                                variantId, currentStock, newStock, deduct, orderId);

                    } catch (Exception exInner) {
                        log.warn("Lỗi khi cập nhật stock cho variantId={} (order={}): {}", oi.getVariantId(), orderId, exInner.getMessage(), exInner);
                        // không throw để không block luồng đặt hàng
                    }
                }
            } catch (Exception exStock) {
                log.warn("Lỗi cập nhật stock sau khi tạo order {}: {}", orderId, exStock.getMessage(), exStock);
            }
            // ====== END update stock ======

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

            // 7) GHN nếu có geo
            log.info("[processCartCheckout] addrId={}, pay={}, toDistrictId={}, toWardCode='{}'",
                    addressId, paymentMethod, toDistrictId, toWardCode);

            boolean ghnCond = (toDistrictId != null && toDistrictId > 0 && toWardCode != null && !toWardCode.isBlank());
            log.info("[processCartCheckout] GHN condition = {}", ghnCond);

            if (ghnCond) {
                // Cập nhật geo cho địa chỉ
                userService.updateAddress(addressId, null, toDistrictId, toWardCode, null, null);

                // 1) Lấy service_id
                var svRes = ghnClientService.availableServices(fromDistrictId, toDistrictId);
                if (svRes == null || svRes.getData() == null || svRes.getData().isEmpty()) {
                    throw new IllegalStateException("Không lấy được service_id từ GHN. Kiểm tra fromDistrictId/toDistrictId.");
                }
                Integer serviceId = svRes.getData().get(0).getServiceId();

                // 2) Tổng cân nặng (gram) — có thể cải tiến: sum theo từng item nếu bạn lưu weight/item
                long totalWeight = Math.max(100, defaultWeight);

                // 3) Tính phí ship
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

                // 4) Items GHN
                List<CreateOrderReq.Item> itemsGHN = order.getOrderItemList().stream()
                        .map(oi -> {
                            CreateOrderReq.Item it = new CreateOrderReq.Item();
                            it.setName(oi.getProductVariant().getProduct().getProductName());
                            it.setQuantity(oi.getQuantity());
                            return it;
                        })
                        .collect(Collectors.toList());

                if (itemsGHN.isEmpty()) {
                    throw new IllegalStateException("GHN yêu cầu ít nhất 1 item trong đơn.");
                }

                // Lưu phí ship (nếu service triển khai)
                try {
                    orderService.updateShippingFee(orderId, BigDecimal.valueOf(shippingFee));
                } catch (Exception e) {
                    log.warn("updateShippingFee not implemented or failed, ignore.");
                }

                // 5) Ai trả phí ship?
                int paymentTypeId = 1; // 1 shop trả, 2 người nhận trả
                int shippingFees = feeRes.getData().getTotal();

                boolean chargeToCustomer = ("COD".equalsIgnoreCase(paymentMethod) && paymentTypeId == 1)
                        || "VNPAY".equalsIgnoreCase(paymentMethod);
                if (chargeToCustomer && shippingFees > 0) {
                    orderService.addShippingToTotal(orderId, BigDecimal.valueOf(shippingFees));
                }

                // Lấy lại order sau khi cộng ship (nếu có)
                order = orderService.getOrderById(orderId);

                // COD amount gửi GHN
                int codAmount = 0;
                if ("COD".equalsIgnoreCase(paymentMethod)) {
                    codAmount = order.getTotalPrice().intValue();
                }

                // 6) Tạo đơn GHN
                CreateOrderReq co = new CreateOrderReq();
                String toName = (order.getRecipientName() != null && !order.getRecipientName().isBlank())
                        ? order.getRecipientName()
                        : (order.getUsers() != null ? order.getUsers().getUsername() : "Khách hàng");
                co.setToName(toName);

                co.setToPhone(order.getPhoneNumber());
                co.setToWardCode(toWardCode);

                User_detail addr = userService.findUserDetalById(addressId);
                String fullAddr = String.format("%s, %s, %s, %s",
                        addr.getAddress(),
                        addr.getWard().getWardName(),
                        addr.getDistrict().getDistrictName(),                 // đảm bảo là “TP Thủ Đức”, không phải “Quận 2”
                        addr.getDistrict().getProvince().getProvinceName()
                );

                co.setToAddress(normalizeAscii(fullAddr)); // dùng hàm bỏ dấu
                co.setFromDistrictId(fromDistrictId);      // ✅ bắt buộc
                co.setFromWardCode(fromWardCode);      // 👉 nên thêm cấu hình: ghn.from-ward-code
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
            return "redirect:/cart/success";

        } catch (Exception ex) {
            ra.addFlashAttribute("message", "Có lỗi khi tạo đơn hàng: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
            ra.addFlashAttribute("type", "danger");

            // render lại trang checkout-cart
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            model.addAttribute("totalWeight", cartService.getTotalWeight());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);

            return "home/checkout-cart";
        }
    }


    @GetMapping("/success")
    public String orderSuccess(RedirectAttributes ra,HttpSession session) {
        ra.addFlashAttribute("message","buy success");
        ra.addFlashAttribute("type","success");
        return "redirect:/";
    }

    private static String normalizeAscii(String s){
        if(s==null) return null;
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+","");
        n = n.replace('\u0110','D').replace('\u0111','d');
        return n.replaceAll("\\s+"," ").trim();
    }

    // ======================
    // 💾 LƯU ĐỊA CHỈ (từ popup ở trang checkout)
    // ======================
    @PostMapping("/checkout/save-address")
    public String saveCartAddress(@RequestParam String phone,
                                  @RequestParam String address,
                                  @RequestParam(required = false) Integer provinceId,
                                  @RequestParam(required = false) Integer districtId,
                                  @RequestParam(required = false) String wardCode,
                                  @RequestParam(required = false, name = "receiverName") String receiverName,
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

        // NEW: mặc định lấy username nếu không nhập
        if (receiverName == null || receiverName.isBlank()) {
            detail.setReceiverName(loggedUser.getUsername());
        } else {
            detail.setReceiverName(receiverName.trim());
        }

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

        return "redirect:/cart/checkout-page";
    }

    // ======================
    // Helpers
    // ======================
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
            if (logged instanceof Users u) return String.valueOf(u.getId());
            return String.valueOf(logged);
        } catch (Exception e) {
            return "unknown";
        }
    }

    @GetMapping("/checkout-page")
    public String checkoutPage(Model model, HttpSession session, RedirectAttributes ra) {
        if (denyIfNotClient(session, ra)) return "redirect:/auth/login";

        Cart cart = cartService.getCart();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            ra.addFlashAttribute("message","Giỏ hàng trống.");
            ra.addFlashAttribute("type","warning");
            return "redirect:/cart";
        }

        // ✅ Tổng giá, số lượng, trọng lượng
        BigDecimal totalPrice = cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO;
        int totalQuantity = cart.getTotalQuantity();
        int totalWeight = cartService.getTotalWeight();

        // ✅ Đưa vào model
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("totalWeight", totalWeight); // <<=== dòng mới
        model.addAttribute("userDetail", userService.getUserDetailFromSession(session));

        return "home/checkout-cart";
    }
}