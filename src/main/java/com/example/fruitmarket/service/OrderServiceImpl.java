package com.example.fruitmarket.service;

import com.example.fruitmarket.Enums.OrderStauts;
import com.example.fruitmarket.Enums.PricingMethod;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.OrderRepo;
import jakarta.servlet.http.HttpSession;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
