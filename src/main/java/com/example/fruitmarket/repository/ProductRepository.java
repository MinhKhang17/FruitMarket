package com.example.fruitmarket.repository;

import com.example.fruitmarket.model.Product;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    Product findProductById(Long id);

    boolean existsByProductName(String productName);

    List<Product> findAllByCreatedAt(LocalDateTime createdAt, Sort sort);

    List<Product> findAllByOrderByCreatedAtDesc(PageRequest of);

    List<Product> findTopByCategoryIdOrderByCreatedAtDesc(Long id, PageRequest of);

    List<Product> findTopByBrandIdOrderByCreatedAtDesc(Long id, PageRequest of);

    List<Product> findByCategory_Id(Long categoryId);
    List<Product> findByBrand_Id(Long brandId);
    List<Product> findByCategory_IdAndBrand_Id(Long categoryId, Long brandId);

    @Query("""
       select p from Product p
       where lower(p.productName) like lower(concat('%', :q, '%'))
          or p.product_description like concat('%', :q, '%')
    """)
    List<Product> search(@Param("q") String q);
}
