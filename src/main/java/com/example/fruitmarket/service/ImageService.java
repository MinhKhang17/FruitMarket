package com.example.fruitmarket.service;

import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.model.Image;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ImageService {
    Image uploadImage(MultipartFile file, ImageType imageType);
    void uploadImagesForVariant(Long variantId, List<MultipartFile> files, ImageType imageType);
}
