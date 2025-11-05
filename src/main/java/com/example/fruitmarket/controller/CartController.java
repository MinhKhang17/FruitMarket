package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.enums.Units;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.service.*;
import com.example.fruitmarket.util.QrUtils;
import com.example.fruitmarket.util.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

/**
 * Controller quản lý giỏ hàng:
 * - Xem giỏ hàng
 * - Thêm / Cập nhật / Xoá / Xoá toàn bộ
 * - Thanh toán chọn lọc hoặc toàn bộ giỏ
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;

    @Autowired
    private VnPayService vnPayService;

    // ======================
    // 📦 HIỂN THỊ GIỎ HÀNG
    // ======================
    @GetMapping
    public String viewCart(Model model, HttpSession session) {
        if (!UserUtil.isLogin(session)) {
            return "redirect:/auth/login";
        }

        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "home/cart/view";
    }

    // ======================
    // ➕ THÊM SẢN PHẨM VÀO GIỎ
    // ======================
    @PostMapping("/add")
    public String addToCart(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(name = "qtyOrWeight", required = false) Double qtyOrWeight,
            @RequestParam(name = "quantity",    required = false) Double quantity,
            @RequestParam(name = "weight",      required = false) Double weight,
            @RequestHeader(value = "Referer", required = false) String referer
    ) {
        double val =
                (weight     != null ? weight     :
                        (quantity   != null ? quantity   :
                                (qtyOrWeight!= null ? qtyOrWeight: 1.0)));
        log.info("🛒 addToCart: productId={}, variantId={}, qtyOrWeight={}", productId, variantId, qtyOrWeight);

        cartService.addToCart(productId, variantId, val);
        return "redirect:" + (referer != null ? referer : "/cart");
    }

    // ======================
    // 🔄 CẬP NHẬT SỐ LƯỢNG / KHỐI LƯỢNG
    // ======================
    @PostMapping("/update")
    public String updateQty(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @RequestParam double qtyOrWeight
    ) {
        log.info("♻️ updateCart: productId={}, variantId={}, qtyOrWeight={}", productId, variantId, qtyOrWeight);

        cartService.updateQuantity(productId, variantId, qtyOrWeight);
        return "redirect:/cart";
    }

    // ======================
    // 🗑️ XOÁ ITEM
    // ======================
    @PostMapping("/remove")
    public String remove(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId
    ) {
        cartService.remove(productId, variantId);
        return "redirect:/cart";
    }

    // ======================
    // ❌ XOÁ TOÀN BỘ GIỎ
    // ======================
    @PostMapping("/clear")
    public String clear() {
        cartService.clear();
        return "redirect:/cart";
    }

    // ======================
    // 💳 THANH TOÁN CÁC ITEM ĐƯỢC CHỌN HOẶC TOÀN BỘ GIỎ
    // ======================
    @PostMapping("/checkout")
    public String checkoutCart(
            @RequestParam(name = "variantIds", required = false) List<Long> variantIds,
            @RequestParam(name = "quantities", required = false) List<Double> quantities,
            Model model,
            HttpSession session,
            RedirectAttributes ra
    ) {
        // 1️⃣ Kiểm tra login
        if (session.getAttribute("loggedUser") == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập trước khi thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        // 2️⃣ Lấy giỏ hàng
        Cart cart = cartService.getCart();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            ra.addFlashAttribute("message", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 3️⃣ Nếu không chọn riêng -> checkout toàn bộ
        if (variantIds == null || variantIds.isEmpty()) {
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);
            return "home/checkout-cart";
        }

        // 4️⃣ Build danh sách item được chọn
        Map<Long, CartItem> cartIndex = new HashMap<>();
        for (CartItem ci : cart.getItems()) {
            if (ci.getVariantId() != null) {
                cartIndex.put(ci.getVariantId(), ci);
            }
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
                sel.setWeight(Math.max(0.1, val));
            } else {
                sel.setQuantity((int) Math.max(1, Math.floor(val)));
            }

            BigDecimal sub = sel.getSubTotal();
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

    // ======================
    // 🧾 XỬ LÝ THANH TOÁN THẬT SỰ (TỪ TRANG CHECKOUT)
    // ======================
    @PostMapping(path = "/process-checkout-from-page", consumes = {"application/x-www-form-urlencoded"})
    public String processCartCheckoutFromCartPage(
            @RequestParam(name = "addressId", required = false) Long addressId,
            @RequestParam(name = "paymentMethod", defaultValue = "COD") String paymentMethod,
            HttpSession session,
            RedirectAttributes ra,
            Model model,
            HttpServletRequest request
    ) {
        // 1️⃣ Kiểm tra đăng nhập
        Object logged = session.getAttribute("loggedUser");
        if (logged == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để tiếp tục thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        // 2️⃣ Lấy giỏ hàng
        Cart cart = cartService.getCart();
        if (cart == null || cart.getItems().isEmpty()) {
            ra.addFlashAttribute("message", "Giỏ hàng trống.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        if (addressId == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn địa chỉ giao hàng.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 3️⃣ Tạo OrderRequest
        OrderRequest orderReq = new OrderRequest();
        orderReq.setAddressId(addressId);
        orderReq.setPaymentMethod(paymentMethod);

        List<OrderRequest.OrderItem> items = new ArrayList<>();

        cart.getItems().forEach(ci -> {
            OrderRequest.OrderItem oi = new OrderRequest.OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setVariantId(ci.getVariantId());
            oi.setName(ci.getName());
            oi.setPrice(ci.getPrice());
            try {
                if (ci.getUnit() != null) {
                    oi.setUnit(Units.valueOf(ci.getUnit().toUpperCase()));
                } else {
                    oi.setUnit(Units.PIECE);
                }
            } catch (Exception e) {
                oi.setUnit(Units.PIECE);
            }

            if ("KILOGRAM".equalsIgnoreCase(ci.getUnit())) {
                oi.setWeight(ci.getWeight());
            } else {
                oi.setQuantity(ci.getQuantity());
            }
            items.add(oi);
        });

        orderReq.setItems(items);
        orderReq.setTotalPrice(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO);
        orderReq.setTotalQuantity(cart.getTotalQuantity());

        // 4️⃣ Tạo đơn hàng
        try {
            Long orderId = orderService.createOrderFromCart(orderReq, session);
            if (orderId == null || orderId <= 0L) {
                throw new IllegalStateException("Tạo đơn thất bại. Vui lòng thử lại.");
            }

            Order order = orderService.getOrderById(orderId);
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                try {
                    String orderInfo = "Thanh toán đơn hàng #" + order.getId();
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

            // ✅ Nếu là COD → xoá giỏ và báo thành công
            cartService.clear();
            ra.addFlashAttribute("message", "Đặt hàng thành công! Mã đơn hàng: " + orderId);
            ra.addFlashAttribute("type", "success");
            return "redirect:/";

        } catch (Exception ex) {
            log.error("❌ Lỗi tạo đơn hàng: {}", ex.getMessage(), ex);
            ra.addFlashAttribute("message", "Lỗi khi tạo đơn hàng: " + ex.getMessage());
            ra.addFlashAttribute("type", "danger");

            // giữ nguyên view checkout
            model.addAttribute("cart", cart);
            model.addAttribute("totalPrice", cart.getTotalPrice());
            model.addAttribute("totalQuantity", cart.getTotalQuantity());
            List<User_detail> userDetails = userService.getUserDetailFromSession(session);
            model.addAttribute("userDetail", userDetails);

            return "home/checkout-cart";
        }
    }

    // ======================
    // 🔐 Helper: tránh log full object
    // ======================
    private String safeUserId(Object logged) {
        try {
            if (logged == null) return "null";
            if (logged instanceof com.example.fruitmarket.model.Users u)
                return String.valueOf(u.getId());
            return String.valueOf(logged);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
