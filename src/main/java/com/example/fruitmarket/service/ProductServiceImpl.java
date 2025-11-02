package com.example.fruitmarket.service;

import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.ProductRepository;
import com.example.fruitmarket.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired private ProductRepository productRepo;
    @Autowired private ProductVariantRepository productVariantRepo;
    @Override
    public Product saveProduct(Product product){
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
    public void deleteById(Long id) {
        productRepo.deleteById(id);
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
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepo.findAll();
        }
        return productRepo.searchByName(keyword.trim());
    }

    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }
}
