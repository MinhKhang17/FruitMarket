package com.example.fruitmarket.controller;

import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.Payment;
import com.example.fruitmarket.service.OrderServiceImpl;
import com.example.fruitmarket.service.PaymentServiceImpl;
import com.example.fruitmarket.service.VnPayService;
import com.example.fruitmarket.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;


@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private VnPayService vnPayService;

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private PaymentServiceImpl paymentService;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    @GetMapping("/vnpay-return")
    public void returnPayment(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String page;

        try {

            String vnp_SecureHash = params.get("vnp_SecureHash");
            Map<String, String> filteredParams = new HashMap<>(params);
            filteredParams.remove("vnp_SecureHash");
            filteredParams.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(filteredParams.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = filteredParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    if (itr.hasNext()) hashData.append('&');
                }
            }

            String computedHash = "";
            try {
                computedHash = vnPayService.hmacSHA512(vnp_HashSecret, hashData.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!computedHash.equalsIgnoreCase(vnp_SecureHash)) {
                page = """
                <h2>Lỗi: Sai chữ ký bảo mật! Dữ liệu không hợp lệ.</h2>
                <a href="http://localhost:8080/">Quay lại hệ thống</a>
                """;
            } else {
                String responseCode = params.get("vnp_ResponseCode");
                String txnRef = params.get("vnp_TxnRef");
                Long orderId = Long.parseLong(txnRef.substring(txnRef.indexOf("OID")+3));
                Order order = orderService.getOrderById(orderId);
                String bankCode = params.get("vnp_BankCode");
                String transactionNo = params.get("vnp_TransactionNo");
                String payDate = params.get("vnp_PayDate");
                BigDecimal amount = new BigDecimal(params.get("vnp_Amount")).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

                if ("00".equals(responseCode)) {
                    Payment payment = Payment.builder()
                            .order(order)
                            .amount(amount)
                            .paymentMethod("VNPAY")
                            .transactionId(transactionNo)
                            .paymentDate(TimeUtils.parsePayDate(payDate))
                            .type("PAY")
                            .build();
                    paymentService.createPayment(payment);
                    order.setPaid(true);
                    orderService.updateOrder(order);
                    page = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Thanh toán thành công</title>
                        <style>
                            body { font-family: Arial, sans-serif; background: #f7f7f7; text-align: center; padding: 50px; }
                            .container { background: #fff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: inline-block; }
                            h2 { color: #4CAF50; }
                            p { margin: 15px 0; font-size: 16px; }
                            .btn { display: inline-block; margin-top: 20px; padding: 10px 20px; background: #2196F3; color: #fff; text-decoration: none; border-radius: 5px; }
                            .btn:hover { background: #1976D2; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h2>Thanh toán thành công!</h2>
                            <p><strong>Booking ID:</strong> %d</p>
                            <p><strong>Số tiền:</strong> %s VND</p>
                            <p><strong>Mã giao dịch:</strong> %s</p>
                            <p><strong>Ngân hàng:</strong> %s</p>
                            <a class="btn" href="http://localhost:8080/">Quay lại trang chủ</a>
                        </div>
                    </body>
                    </html>
                    """.formatted(orderId, amount, transactionNo, bankCode);
                } else {
                    page = """
                        <!DOCTYPE html>
                        <html lang="vi">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Thanh toán thất bại</title>
                            <style>
                                body { font-family: Arial, sans-serif; background: #f7f7f7; text-align: center; padding: 50px; }
                                .container { background: #fff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: inline-block; }
                                h2 { color: #f44336; }
                                .btn { display: inline-block; margin-top: 20px; padding: 10px 20px; background: #2196F3; color: #fff; text-decoration: none; border-radius: 5px; }
                                .btn:hover { background: #1976D2; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h2>Thanh toán thất bại!</h2>
                                <p>Mã lỗi: %s</p>
                                <a class="btn" href="http://localhost:8080/">Quay lại trang chủ</a>
                            </div>
                        </body>
                        </html>
                        """.formatted(responseCode);
                }
            }
        } catch (Exception e) {
            page = """
            <h2>Lỗi hệ thống: %s</h2>
            <a href="http://localhost:8080/">Quay lại hệ thống</a>
            """.formatted(e.getMessage());
        }

        response.getWriter().write(page);
    }
}





