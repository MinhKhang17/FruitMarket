package com.example.fruitmarket.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.fruitmarket.Enums.ImageType;
import com.example.fruitmarket.model.Image;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.repository.ImageRepository;
import com.example.fruitmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    private final ImageRepository imageRepository;
    private final ProductRepository productRepository;


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
        }    }

    @Override
    public void uploadImagesForProduct(Long productId, List<MultipartFile> files, ImageType imageType) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        for (MultipartFile file : files) {
            Image img = uploadImage(file, imageType);
//            product.getImages().add(img);
        }

        productRepository.save(product);
    }
}
