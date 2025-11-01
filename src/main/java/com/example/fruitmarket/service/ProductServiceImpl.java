package com.example.fruitmarket.service;

import com.example.fruitmarket.Dto.ProductDTO;
import com.example.fruitmarket.mapper.FruitMapper;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired private ProductRepository productRepo;
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
}
