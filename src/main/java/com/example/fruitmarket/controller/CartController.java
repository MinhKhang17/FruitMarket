package com.example.fruitmarket.controller;

import com.example.fruitmarket.Dto.OrderRequest;
import com.example.fruitmarket.Util.UserUtil;
import com.example.fruitmarket.model.Cart;
import com.example.fruitmarket.model.CartItem;
import com.example.fruitmarket.model.User_detail;
import com.example.fruitmarket.service.CartService;
import com.example.fruitmarket.service.OrderService;
import com.example.fruitmarket.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService;
    private final OrderService orderService;
    @GetMapping
    public String viewCart(Model model,HttpSession session) {
        if(!UserUtil.isLogin(session)){
            return "redirect:/auth/login";
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
            @RequestHeader(value = "Referer", required = false) String referer
    ) {
        cartService.addToCart(productId, variantId, qty);
        // redirect về trang gọi thêm (referer) hoặc /cart
        return "redirect:" + (referer != null ? referer : "/cart");
    }

    @PostMapping("/update")
    public String updateQty(@RequestParam Long productId,
                            @RequestParam(required=false) Long variantId,
                            @RequestParam int qty) {
        cartService.updateQuantity(productId, variantId, qty);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long productId,
                         @RequestParam(required=false) Long variantId) {
        cartService.remove(productId, variantId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clear() {
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
            HttpSession session,
            RedirectAttributes ra,
            Model model
    ) {
        // 1. Kiểm tra đăng nhập
        Object logged = session.getAttribute("loggedUser");
        if (logged == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập để tiếp tục thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        // 2. Lấy giỏ hàng hiện tại
        Cart cart = cartService.getCart();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            ra.addFlashAttribute("message", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 3. Kiểm tra addressId
        if (addressId == null) {
            log.warn("Checkout attempted with null addressId for userId: {}", safeUserId(logged));
            ra.addFlashAttribute("message", "Vui lòng chọn địa chỉ giao hàng.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/cart";
        }

        // 4. Build OrderRequest từ toàn bộ cart (log thông tin để debug)
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
            oi.setQuantity(ci.getQuantity());
            items.add(oi);
        });
        orderReq.setItems(items);
        orderReq.setTotalPrice(cart.getTotalPrice() != null ? cart.getTotalPrice() : BigDecimal.ZERO);
        orderReq.setTotalQuantity(cart.getTotalQuantity());

        log.info("Creating order for userId={}, addressId={}, payment={}, totalItems={}, totalPrice={}",
                safeUserId(logged), addressId, paymentMethod, items.size(), orderReq.getTotalPrice());

        // 5. Gọi OrderService để tạo đơn
        try {
            Long orderId = orderService.createOrderFromCart(orderReq, session);

            if (orderId == null || orderId <= 0L) {
                log.error("OrderService returned invalid orderId: {} for userId: {}", orderId, safeUserId(logged));
                throw new IllegalStateException("Tạo đơn thất bại (invalid orderId). Vui lòng thử lại.");
            }

            // 6. Xóa giỏ hàng sau khi đặt thành công
            cartService.clear();

            ra.addFlashAttribute("message", "Đặt hàng thành công! Mã đơn hàng: " + orderId);
            ra.addFlashAttribute("type", "success");

            log.info("Order created successfully: orderId={}, userId={}", orderId, safeUserId(logged));
            return "redirect:/";
        } catch (Exception ex) {
            log.error("Error creating order for userId={}. msg={}", safeUserId(logged), ex.getMessage(), ex);
            ra.addFlashAttribute("message", "Có lỗi khi tạo đơn hàng: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
            ra.addFlashAttribute("type", "danger");

            // trả về checkout page với dữ liệu hiện tại để người dùng thử lại
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

}
