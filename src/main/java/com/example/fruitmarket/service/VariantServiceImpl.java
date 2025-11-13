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
import java.math.BigDecimal;
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
        // Tìm sản phẩm theo ID
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + productId));

        // Kiểm tra null cho variant
        if (variant == null) {
            throw new IllegalArgumentException("Biến thể sản phẩm không được để trống");
        }

        // Kiểm tra giá
        if (variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0");
        }

        // Kiểm tra tồn kho
        if (variant.getStock() <= 0) {
            throw new IllegalArgumentException("Số lượng tồn kho phải lớn hơn 0");
        }

        // Gắn product cho variant
        variant.setProduct(product);

        // Lưu variant
        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Upload ảnh đầu tiên nếu có
        if (files != null && !files.isEmpty()) {
            MultipartFile firstFile = files.get(0);
            if (!firstFile.isEmpty()) {
                imageService.uploadImageForVariant(savedVariant.getId(), firstFile, imageType);
            }
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
    public void updateStatusToActive(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + variantId));
        variant.setStatus(ProductStatus.ACTIVE);
        productVariantRepository.save(variant);
    }

    @Override
    public ProductVariant findById(Long variantId) {
        return productVariantRepository.findById(variantId).orElse(null);
    }

    @Override
    public ProductVariant update(ProductVariant variant, List<MultipartFile> files, ImageType imageType) throws IOException {
        // 🔍 Lấy bản gốc từ DB
        ProductVariant existing = productVariantRepository.findById(variant.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể có ID: " + variant.getId()));

        existing.setVariant_name(variant.getVariant_name());
        existing.setPrice(variant.getPrice());
        existing.setStock(variant.getStock());

        // Nếu có Product trong variant thì cập nhật lại (nếu không thì giữ nguyên)
        if (variant.getProduct() != null) {
            Product product = productRepository.findById(variant.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm cho biến thể này"));
            existing.setProduct(product);
        }

        // ✅ Upload lại ảnh nếu có file mới
        if (files != null && !files.isEmpty() && !files.get(0).isEmpty()) {
            MultipartFile firstFile = files.get(0);
            imageService.uploadImageForVariant(existing.getId(), firstFile, imageType);
        }

        // ✅ Lưu lại biến thể đã cập nhật
        return productVariantRepository.save(existing);
    }
}
