package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Order;
import com.example.fruitmarket.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    List<Order> findAllByUsersOrderByIdDesc(Users users);
    Order findOrderById(Long id); // Có thể trả về null
    Optional<Order> findById(Long id); // Trả về Optional
    List<Order> findAll();

    @Modifying
    @Query("update Order o set o.shippingFee = :fee where o.id = :orderId")
    int updateShippingFee(@Param("orderId") Long orderId,
                          @Param("fee") BigDecimal fee);

    @Modifying
    @Query("update Order o set o.totalPrice = coalesce(o.totalPrice,0) + :fee, " +
            "o.shippingFee = coalesce(o.shippingFee,0) + :fee " +
            "where o.id = :orderId")
    void addShippingToTotal(@Param("orderId") Long orderId,
                            @Param("fee") BigDecimal fee);
}