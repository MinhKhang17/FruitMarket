package com.example.fruitmarket.service;

import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.repository.ProductRepository;
import com.example.fruitmarket.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    public List<Product> findByFilters(Long categoryId, Long brandId) {
        List<Product> products = productRepo.findAll();

        // Lọc theo category nếu có
        if (categoryId != null) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        // Lọc theo brand nếu có
        if (brandId != null) {
            products = products.stream()
                    .filter(p -> p.getBrand() != null && p.getBrand().getId().equals(brandId))
                    .collect(Collectors.toList());
        }

        // Chỉ lấy products có variants
        return products.stream()
                .filter(p -> p.getVariants() != null && !p.getVariants().isEmpty())
                .collect(Collectors.toList());
    }
}
