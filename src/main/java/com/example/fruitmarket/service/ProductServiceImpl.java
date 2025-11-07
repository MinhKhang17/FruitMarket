package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.ProductDTO;
import com.example.fruitmarket.dto.ProductDTO;
import com.example.fruitmarket.enums.ProductStatus;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.repository.ProductRepository;
import com.example.fruitmarket.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired private ProductRepository productRepo;
    @Autowired private ProductVariantRepository productVariantRepo;

    @Override
    public Product saveProduct(Product product){
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                variant.setProduct(product);
            }
        }

        return productRepo.save(product);
    }

    @Override
    public List<Product> findAll() {
        return productRepo.findAll();
    }

    @Override
    public Product findById(Long id) {
        return productRepo.findProductById(id);
    }


    @Override
    public ProductDTO findAllProductWithProductVariant(long id) {
        return FruitMapper.toProductDTO(productRepo.findById(id).orElseThrow());
    }

    @Override
    public ProductVariant findProductVariantById(long productVariantId) {
        return productVariantRepo.findById(productVariantId).orElseThrow();
    }

    @Override
    public void decreaseStock(Long variantId, Integer quantity) {
        ProductVariant productVariant = findProductVariantById(variantId);
        productVariant.setStock(productVariant.getStock() - quantity);
        productVariantRepo.save(productVariant);
    }

    public List<Product> findLatestProducts(int limit) {
        return productRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Override
    public Map<Long, List<Product>> findProductsGroupedByCategory(List<Categorys> categories, int perCategory) {
        Map<Long, List<Product>> map = new LinkedHashMap<>();
        for (Categorys c : categories) {
            List<Product> list = productRepo.findTopByCategoryIdOrderByCreatedAtDesc(c.getId(), PageRequest.of(0, perCategory));
            map.put(c.getId(), list);
        }
        return map;
    }
    @Override
    public Map<Long, List<Product>> findProductsGroupedByBrand(List<Brands> brands, int perBrand) {
        Map<Long, List<Product>> map = new LinkedHashMap<>();
        for (Brands b : brands) {
            List<Product> list = productRepo.findTopByBrandIdOrderByCreatedAtDesc(b.getId(), PageRequest.of(0, perBrand));
            map.put(b.getId(), list);
        }
        return map;
    }

    @Override
    public List<Product> findByCategory(Long categoryId) {
        if (categoryId == null) return Collections.emptyList();
        return productRepo.findByCategory_Id(categoryId);
    }

    @Override
    public List<Product> findByBrand(Long brandId) {
        if (brandId == null) return Collections.emptyList();
        return productRepo.findByBrand_Id(brandId);
    }

    @Override
    public List<Product> findByCategoryAndBrand(Long categoryId, Long brandId) {
        if (categoryId == null && brandId == null) return Collections.emptyList();
        if (categoryId != null && brandId != null) {
            return productRepo.findByCategory_IdAndBrand_Id(categoryId, brandId);
        }
        if (categoryId != null) {
            return productRepo.findByCategory_Id(categoryId);
        }
        return productRepo.findByBrand_Id(brandId);
    }

    @Override
    public List<Product> search(String keyword) {
        if (keyword == null) return Collections.emptyList();
        String q = keyword.trim();
        if (q.isEmpty()) return Collections.emptyList();
        return productRepo.search(q);
    }

    @Override
    public void updateProductStatusToInactive(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        product.setStatus(ProductStatus.INACTIVE);
        productRepo.save(product);
    }

    @Override
    public void updateProductStatusToActive(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        product.setStatus(ProductStatus.ACTIVE);
        productRepo.save(product);
    }

}
