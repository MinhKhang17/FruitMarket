package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.OrderRequest;
import com.example.fruitmarket.enums.OrderStauts;
import com.example.fruitmarket.enums.PricingMethod;
import com.example.fruitmarket.enums.Units;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.OrderRepo;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
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
        orderItem.setQuantity(quantity);
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
        double computedWeight = 0.0;
        int computedQty = 0;

        if (orderReq.getItems() == null || orderReq.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        for (OrderRequest.OrderItem reqItem : orderReq.getItems()) {
            Long variantId = reqItem.getVariantId();
            ProductVariant pv = productService.findProductVariantById(variantId);
            if (pv == null) {
                throw new IllegalArgumentException("Không tìm thấy biến thể sản phẩm ID: " + variantId);
            }

            OrderItem oi = new OrderItem();
            oi.setProductVariant(pv);
            oi.setPrice(reqItem.getPrice() != null ? reqItem.getPrice() : pv.getPrice());

            Units unit = reqItem.getUnit() != null ? reqItem.getUnit() : Units.PIECE;
            oi.setUnit(unit);

            if (unit == Units.KILOGRAM) {
                double w = reqItem.getWeight() != null ? reqItem.getWeight() : 0.1;
                oi.setWeight(w);
                computedWeight += w;
                oi.setQuantity(null);
            } else {
                int q = reqItem.getQuantity() != null ? reqItem.getQuantity() : 1;
                oi.setQuantity(q);
                computedQty += q;
                oi.setWeight(null);
            }

            BigDecimal sub = oi.getSubTotal();
            computedTotal = computedTotal.add(sub);
            computedQty += (oi.getUnit() == Units.KILOGRAM ? 0 : oi.getQuantity());

            orderItems.add(oi);
            System.out.println("==> unit=" + reqItem.getUnit() + ", weight=" + reqItem.getWeight());

        }

        // set total trên order
        order.setTotalPrice(computedTotal);
        order.setTotalQuantity(computedQty);
        order.setTotalWeight(computedWeight);

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
        if (order.getUsers() == null || order.getUsers().getId() != user.getId()) {
            throw new IllegalStateException("Bạn không có quyền xem đơn hàng này.");
        }
        return order; // đã fetch đủ items/variant/product nhờ @EntityGraph
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }
}
