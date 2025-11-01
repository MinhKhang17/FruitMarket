//package com.example.fruitmarket.controller;
//
//import com.example.fruitmarket.service.VnPayService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import java.math.BigDecimal;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//
//@Controller
//@RequestMapping("/vnpay")
//public class PaymentController {
//
//    @Autowired
//    private VnPayService vnPayService;
//
//    @Autowired
//    private BookingService bookingService;
//
//    @Autowired
//    private PaymentService paymentService;
//
//    @Value("${vnpay.hashSecret}")
//    private String vnp_HashSecret;
//
//    @GetMapping("/return")
//    public String returnPayment(@RequestParam Map<String, String> params, Model model) {
//        String vnp_SecureHash = params.get("vnp_SecureHash");
//
//        Map<String, String> filteredParams = new HashMap<>(params);
//        filteredParams.remove("vnp_SecureHash");
//        filteredParams.remove("vnp_SecureHashType");
//
//        List<String> fieldNames = new ArrayList<>(filteredParams.keySet());
//        Collections.sort(fieldNames);
//        StringBuilder hashData = new StringBuilder();
//        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
//            String fieldName = itr.next();
//            String fieldValue = filteredParams.get(fieldName);
//            if (fieldValue != null && !fieldValue.isEmpty()) {
//                hashData.append(fieldName).append('=')
//                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
//                if (itr.hasNext()) hashData.append('&');
//            }
//        }
//
//        String computedHash = vnPayService.hmacSHA512(vnp_HashSecret, hashData.toString());
//        if (!computedHash.equalsIgnoreCase(vnp_SecureHash)) {
//            model.addAttribute("message", "Sai chữ ký xác thực!");
//            return "payment-fail";
//        }
//
//        String responseCode = params.get("vnp_ResponseCode");
//        String txnRef = params.get("vnp_TxnRef");
//        String transactionNo = params.get("vnp_TransactionNo");
//        String payDate = params.get("vnp_PayDate");
//        String bankCode = params.get("vnp_BankCode");
//        BigDecimal amount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100));
//
//        if ("00".equals(responseCode)) {
//            int bookingId = Integer.parseInt(txnRef);
//            Booking booking = bookingService.findById(bookingId);
//            booking.setStatus("PAID");
//            bookingService.save(booking);
//
//            Payment payment = new Payment();
//            payment.setBooking(booking);
//            payment.setAmount(amount);
//            payment.setReferenceCode(transactionNo);
//            payment.setTransactionDate(LocalDateTime.now());
//            payment.setBankCode(bankCode);
//            paymentService.save(payment);
//
//            model.addAttribute("booking", booking);
//            model.addAttribute("amount", amount);
//            model.addAttribute("bank", bankCode);
//            model.addAttribute("transactionNo", transactionNo);
//            model.addAttribute("payDate", payDate);
//
//            return "payment-success";
//        } else {
//            model.addAttribute("message", "Thanh toán thất bại! Mã lỗi: " + responseCode);
//            return "payment-fail";
//        }
//    }
//}
//
//
//
//
//
