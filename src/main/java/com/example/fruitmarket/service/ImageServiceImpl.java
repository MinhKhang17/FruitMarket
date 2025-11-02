package com.example.fruitmarket.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.model.Image;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.repository.ImageRepository;
import com.example.fruitmarket.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final ProductVariantRepository variantRepository;

    // ✅ inject từ CloudinaryConfig.java
    private final Cloudinary cloudinary;

    @Override
    public Image uploadImage(MultipartFile file, ImageType imageType) throws IOException {
        // upload lên cloudinary (sử dụng folder riêng)
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap("folder", "fruitmarket_uploads"));

        String url = uploadResult.get("secure_url").toString();

        Image image = new Image();
        image.setUrl(url);
        image.setImageType(imageType);

        return imageRepository.save(image);
    }

    @Override
    public void uploadImageForVariant(Long variantId, MultipartFile file, ImageType imageType) throws IOException {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));

        // Upload ảnh lên Cloudinary
        Image image = uploadImage(file, imageType);

        // Gắn ảnh với variant (1-1)
        variant.setImage(image);
        variantRepository.save(variant);
    }
}
