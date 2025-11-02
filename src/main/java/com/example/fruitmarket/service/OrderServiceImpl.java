package com.example.fruitmarket.service;

import com.example.fruitmarket.Dto.OrderRequest;
import com.example.fruitmarket.Enums.OrderStauts;
import com.example.fruitmarket.Enums.PricingMethod;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.OrderRepo;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired UserService userService;
    @Autowired OrderRepo orderRepo;
    @Autowired ProductService productService;
    @Override
    public Order createOrder(HttpSession session, ProductVariant variant, Integer quantity, Long addressId, String paymentMethod) {
        Users user = (Users)session.getAttribute("loggedUser");

        User_detail userDetail = userService.findUserDetalById(addressId);

        Order order = new Order();
        order.setUsers(user);
        order.setAddress(userDetail.getAddress());
        order.setPhoneNumber(userDetail.getPhone());
        order.setPricingMethod(PricingMethod.valueOf(paymentMethod));
        order.setOrderStauts(OrderStauts.PENDING);

        //xu lu order item
        OrderItem orderItem = new OrderItem();
        orderItem.setQuanity(quantity);
        orderItem.setProductVariant(productService.findProductVariantById(variant.getId()));
        order.setTotalPrice(orderItem.getProductVariant().getPrice());
        List<OrderItem> orderItems = order.getOrderItemList();
        orderItems.add(orderItem);
        order.setOrderItemList(orderItems);
        return orderRepo.save(order);
    }

    @Override
    @Transactional
    public Long createOrderFromCart(OrderRequest orderReq, HttpSession session) {
        // Lấy user từ session
        Users user = (Users) session.getAttribute("loggedUser");
        if (user == null) {
            throw new IllegalStateException("User not logged in");
        }

        // Validate addressId
        Long addressId = orderReq.getAddressId();
        if (addressId == null) {
            throw new IllegalArgumentException("AddressId is required");
        }
        User_detail userDetail = userService.findUserDetalById(addressId);
        if (userDetail == null) {
            throw new IllegalArgumentException("Invalid addressId: " + addressId);
        }

        // Tạo đối tượng Order
        Order order = new Order();
        order.setUsers(user);
        order.setAddress(userDetail.getAddress());
        order.setPhoneNumber(userDetail.getPhone());

        // set pricing method (nếu enum không khớp sẽ ném IllegalArgumentException)
        try {
            order.setPricingMethod(PricingMethod.valueOf(orderReq.getPaymentMethod()));
        } catch (Exception ex) {
            // fallback hoặc throw rõ ràng
            throw new IllegalArgumentException("Invalid payment method: " + orderReq.getPaymentMethod());
        }
        order.setOrderStauts(OrderStauts.PENDING);

        // Build OrderItem list và tính tổng
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal computedTotal = BigDecimal.ZERO;
        int computedQty = 0;

        if (orderReq.getItems() == null || orderReq.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (OrderRequest.OrderItem reqItem : orderReq.getItems()) {
            // Lấy variant (ưu tiên variantId nếu có)
            Long variantId = reqItem.getVariantId();
            ProductVariant pv = null;
            if (variantId != null) {
                pv = productService.findProductVariantById(variantId);
                if (pv == null) {
                    throw new IllegalArgumentException("ProductVariant not found: " + variantId);
                }
            } else {
                // Nếu không có variantId, cố gắng tìm variant mặc định theo productId
                if (reqItem.getProductId() != null) {
                    pv = productService.findProductVariantById(reqItem.getProductId()); // <-- giả định method
                }
                if (pv == null) {
                    throw new IllegalArgumentException("No variant specified and no default variant found for product: " + reqItem.getProductId());
                }
            }

            // TODO: nếu bạn có hệ thống tồn kho, kiểm tra stock ở đây
            // if (pv.getStock() < reqItem.getQuantity()) { throw new IllegalStateException("Insufficient stock"); }

            OrderItem oi = new OrderItem();
            oi.setProductVariant(pv);
            oi.setQuanity(reqItem.getQuantity() == null ? 1 : reqItem.getQuantity());
            // Lưu lại giá tại thời điểm đặt
            if (reqItem.getPrice() != null) {
                oi.setPrice(reqItem.getPrice());
            } else {
                // nếu DTO không có price, lấy từ variant
                oi.setPrice(pv.getPrice());
            }
            oi.setProductVariant(productService.findProductVariantById(reqItem.getVariantId())); // nếu OrderItem có field productName; nếu không, sửa lại

            // tính subtotal
            BigDecimal price = oi.getProductVariant().getPrice() != null ? oi.getProductVariant().getPrice() : BigDecimal.ZERO;
            BigDecimal qtyBd = BigDecimal.valueOf(oi.getQuanity());
            BigDecimal sub = price.multiply(qtyBd);

            computedTotal = computedTotal.add(sub);
            computedQty += oi.getQuanity();

            orderItems.add(oi);
        }

        // set total trên order
        order.setTotalPrice(computedTotal);
        order.setTotalQuantity(computedQty);

        // gán list order items cho order (nếu order.getOrderItemList() trả null, khởi tạo mới)
        if (order.getOrderItemList() == null) {
            order.setOrderItemList(new ArrayList<>());
        }
        order.getOrderItemList().addAll(orderItems);

        // Lưu order (với cascade/persist cho orderItems nếu config entity đúng)
        Order saved = orderRepo.save(order);

        // TODO: Nếu cần trừ kho, thực hiện sau khi save hoặc trong transaction (với locking)
        // Ví dụ: productService.decreaseStock(pv.getId(), quantity);

        return saved.getId();
    }
    @Override
    public Order getOrderById(Long Id) {
        return orderRepo.findById(Id);
    }

    @Override
    public void updateOrder(Order order) {
        orderRepo.save(order);
    }
}
