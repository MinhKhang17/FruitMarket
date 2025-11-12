package com.example.fruitmarket.controller;

import com.example.fruitmarket.dto.CheckoutProcessRequest;
import com.example.fruitmarket.dto.CheckoutRequest;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.service.*;
import com.example.fruitmarket.util.AuthUtils;
import com.example.fruitmarket.util.QrUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class BuyController {
    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private OrderService orderService;
    @Autowired
    private VnPayService vnPayService;
    @Autowired private DistrictService districtService;
    @Autowired private ProvinceService provinceService;
    @Autowired private WardService wardService;
    @Autowired private GhnClientService ghnClientService;

    @Value("${ghn.from-district-id}")
    private int fromDistrictId;

    @PostMapping("/checkout")
    public String checkout(@ModelAttribute CheckoutRequest checkoutRequest,
                           Model model,
                           HttpSession session,
                           RedirectAttributes ra) {

        // 1. Kiểm tra login trước

        if (session.getAttribute("loggedUser")==null) {
            ra.addFlashAttribute("message","Bạn phải đăng nhập trước");
            ra.addFlashAttribute("type","danger");
            return "redirect:/";
        }
        if (!AuthUtils.isClient(session)) {
            ra.addFlashAttribute("message", "Chỉ tài khoản khách hàng (CLIENT) đang hoạt động mới được phép đặt hàng.");
            ra.addFlashAttribute("type", "warning");
            return "redirect:/";
        }

        var productVariant = FruitMapper.toProductCheckout(productService.findProductVariantById(checkoutRequest.getProduct_variant_id()));
        model.addAttribute("productVariant", productVariant);
        model.addAttribute("quantity", checkoutRequest.getQuantity());

        session.setAttribute("productVariant", productVariant);
        session.setAttribute("quantity", checkoutRequest.getQuantity());
        List<User_detail> user = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail",user);

        return "home/checkout";
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@ModelAttribute CheckoutProcessRequest checkoutRequest,
                                  HttpSession session,
                                  RedirectAttributes ra,
                                  Model model,
                                  HttpServletRequest request) {

        Users logged = (Users) session.getAttribute("loggedUser");
        if (logged == null) {
            ra.addFlashAttribute("message", "Bạn cần đăng nhập trước khi thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/auth/login";
        }

        Long variantId     = checkoutRequest.getVariantId();
        Integer quantity   = checkoutRequest.getQuantity();
        Long addressId     = checkoutRequest.getAddressId();
        String paymentMethod = checkoutRequest.getPaymentMethod();

        // từ form (hidden)
        Integer serviceId    = checkoutRequest.getServiceId();      // có thể null
        Integer toDistrictId = checkoutRequest.getToDistrictId();   // có thể null
        String  toWardCode   = checkoutRequest.getToWardCode();     // có thể null
        BigDecimal shippingFee = checkoutRequest.getShippingFee();  // có thể null

        var variant = productService.findProductVariantById(variantId);
        if (variant == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/";
        }

        if (quantity == null || quantity <= 0 || quantity > variant.getStock()) {
            ra.addFlashAttribute("message", "Số lượng không hợp lệ.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/product/" + variant.getProduct().getId();
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            ra.addFlashAttribute("message", "Vui lòng chọn phương thức thanh toán.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }

        if (addressId == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn địa chỉ giao hàng.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }

        // Lấy địa chỉ để có ward/district nếu client chưa gửi
        User_detail addr = userService.findUserDetalById(addressId);
        if (addr == null || addr.getWard() == null || addr.getDistrict() == null) {
            ra.addFlashAttribute("message", "Địa chỉ thiếu thông tin phường/xã hoặc quận/huyện.");
            ra.addFlashAttribute("type", "danger");
            return "redirect:/checkout";
        }
        if (toDistrictId == null) toDistrictId = addr.getDistrict().getDistrictId();
        if (toWardCode == null)   toWardCode   = addr.getWard().getWardCode();

        // Nếu shippingFee rỗng hoặc 0 → tính lại tại BE cho chắc chắn
        if (shippingFee == null || shippingFee.signum() < 0) shippingFee = BigDecimal.ZERO;
        if (shippingFee.signum() == 0) {
            try {
                // chọn serviceId nếu thiếu
                if (serviceId == null) {
                    var svRes = ghnClientService.availableServices(/** fromDistrictId */ fromDistrictId, toDistrictId);
                    if (svRes != null && svRes.getData() != null && !svRes.getData().isEmpty()) {
                        serviceId = svRes.getData().get(0).getServiceId();
                    }
                }
                if (serviceId != null) {
                    int weight = Math.max(1, quantity) * 500; // 500g/item (tuỳ bạn)
                    int length = 20, width = 15, height = 10;
                    BigDecimal goods = variant.getPrice().multiply(BigDecimal.valueOf(quantity));

                    var feeRes = ghnClientService.calculateFee(
                            fromDistrictId, toDistrictId, toWardCode,
                            serviceId, weight, length, width, height,
                            goods.intValue()
                    );
                    Integer fee = (feeRes != null && feeRes.getData() != null) ? feeRes.getData().getTotal() : null;
                    shippingFee = BigDecimal.valueOf(fee != null ? fee : 0);
                }
            } catch (Exception ex) {
                // nếu GHN lỗi: để 0, tiếp tục
                shippingFee = BigDecimal.ZERO;
            }
        }

        // Tạo order: truyền shippingFee vào service để tính tổng chuẩn
        Order order = orderService.createOrder(
                session, variant, quantity, addressId, paymentMethod, shippingFee, serviceId
        );

        // trừ tồn
        productService.decreaseStock(variantId, quantity);

        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            try {
                String orderInfo = "Thanh toan don hang #" + order.getId();
                String paymentUrl = vnPayService.createPaymentUrl(
                        request,
                        order.getTotalPrice(), // đã gồm ship
                        orderInfo,
                        order.getId()
                );

                String qrBase64 = QrUtils.generateQrBase64(paymentUrl);
                model.addAttribute("paymentUrl", paymentUrl);
                model.addAttribute("qrBase64", qrBase64);
                model.addAttribute("order", order);
                model.addAttribute("amount", order.getTotalPrice());

                return "home/payment_qr";
            } catch (Exception e) {
                e.printStackTrace();
                ra.addFlashAttribute("message", "Lỗi tạo QR thanh toán VNPAY: " + e.getMessage());
                ra.addFlashAttribute("type", "danger");
                return "redirect:/";
            }
        }

        ra.addFlashAttribute("message", "Đặt hàng thành công!");
        return "redirect:/order/success";
    }

    @GetMapping("/order/success")
    public String orderSuccess(RedirectAttributes ra,HttpSession session) {
        ra.addFlashAttribute("message","buy success");
        ra.addFlashAttribute("type","success");
        return "redirect:/";
    }

    @PostMapping("/checkout/save-address")
    public String saveAddress(@RequestParam String phone,
                              @RequestParam String address,
                              @RequestParam(required = false) Integer provinceId,
                              @RequestParam(required = false) Integer districtId,
                              @RequestParam(required = false) String wardCode,
                              @RequestParam(required = false) String receiverName, // NEW
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

        // NEW: lưu tên người nhận (fallback username nếu để trống)
        if (receiverName != null && !receiverName.isBlank()) {
            detail.setReceiverName(receiverName.trim());
        } else {
            detail.setReceiverName(loggedUser.getUsername()); // mặc định
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

        return "redirect:/checkout";
    }

    @GetMapping("/checkout")
    public String getCheckoutPage(HttpSession session, Model model) {
        Users loggedUser = (Users) session.getAttribute("loggedUser");
        if (loggedUser == null) return "redirect:/auth/login";

        List<User_detail> userDetails = userService.getUserDetailFromSession(session);
        model.addAttribute("userDetail", userDetails);

        // nếu bạn có lưu productVariant, quantity trong session thì lấy lại
        Object productVariant = session.getAttribute("productVariant");
        Object quantity = session.getAttribute("quantity");
        model.addAttribute("productVariant", productVariant);
        model.addAttribute("quantity", quantity);

        return "home/checkout";
    }
}
