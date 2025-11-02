package com.example.fruitmarket.service;

import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageService {
    Image uploadImage(MultipartFile file, ImageType imageType) throws IOException;
    void uploadImageForVariant(Long variantId, MultipartFile file, ImageType imageType) throws IOException;
}
