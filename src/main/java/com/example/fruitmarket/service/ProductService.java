package com.example.fruitmarket.service;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.fruitmarket.dto.ProductDTO;
import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.dto.ProductDTO;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

public interface ProductService {
    Product saveProduct(Product product);

    List<Product> findAll();

    Product findById(Long id);

    ProductDTO findAllProductWithProductVariant(long id);

    ProductVariant findProductVariantById(long productVariantId);

    void decreaseStock(Long variantId, Integer quantity);

    List<Product> findLatestProducts(int i);

    Map<Long, List<Product>> findProductsGroupedByCategory(List<Categorys> categories, int i);

    Map<Long, List<Product>> findProductsGroupedByBrand(List<Brands> brands, int perBrand);

    List<Product> findByCategory(Long categoryId);
    List<Product> findByBrand(Long brandId);
    List<Product> findByCategoryAndBrand(Long categoryId, Long brandId);
    List<Product> search(String keyword);
    void updateProductStatusToInactive(Long productId);
    void updateProductStatusToActive(Long productId);

}
