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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;
    private final ProductVariantRepository variantRepository; // dùng variant repo

    private final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dtlvxcxdw",
            "api_key", "687268262255632",
            "api_secret", "q4hktNoHjL8GOAkHO-MKJS8GeIg",
            "secure", true
    ));

    @Override
    public Image uploadImage(MultipartFile file, ImageType imageType) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "fruitmarket_uploads"));
            String url = uploadResult.get("secure_url").toString();

            Image image = new Image();
            image.setUrl(url);
            image.setImageType(imageType);

            return imageRepository.save(image);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi upload ảnh lên Cloudinary", e);
        }
    }

    @Override
    public void uploadImagesForVariant(Long variantId, List<MultipartFile> files, ImageType imageType) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể"));

        List<Image> uploadedImages = new ArrayList<>();

        for (MultipartFile file : files) {
            Image img = uploadImage(file, imageType);
            img.setVariant(variant);              // Gắn variant
            uploadedImages.add(imageRepository.save(img));
        }

        // Cập nhật list images cho variant (nếu bạn có mappedBy = "variant")
        variant.getImages().addAll(uploadedImages);
        variantRepository.save(variant);
    }
}
