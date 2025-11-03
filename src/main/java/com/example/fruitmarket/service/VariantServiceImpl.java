package com.example.fruitmarket.service;

import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.enums.ProductStatus;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.repository.ProductRepository;
import com.example.fruitmarket.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VariantServiceImpl implements VariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ImageService imageService;

    @Override
    public ProductVariant createVariant(Long productId,
                                        ProductVariant variant,
                                        List<MultipartFile> files,
                                        ImageType imageType) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + productId));

        variant.setProduct(product);

        ProductVariant savedVariant = productVariantRepository.save(variant);

        if (files != null && !files.isEmpty()) {
            MultipartFile firstFile = files.get(0);
            imageService.uploadImageForVariant(savedVariant.getId(), firstFile, imageType);
        }

        return savedVariant;
    }

    @Override
    public void updateStatusToInactive(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + variantId));
        variant.setStatus(ProductStatus.INACTIVE);
        productVariantRepository.save(variant);
    }

    @Override
    public ProductVariant findById(Long variantId) {
        return productVariantRepository.findById(variantId).orElse(null);
    }
}
