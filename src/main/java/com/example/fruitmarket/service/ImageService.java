package com.example.fruitmarket.service;

import com.example.fruitmarket.Enums.ImageType;
import com.example.fruitmarket.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageService {
    Image uploadImage(MultipartFile file, ImageType imageType);
    void uploadImagesForProduct(Long productId, List<MultipartFile> files, ImageType imageType);

}
