package com.example.fruitmarket.service;

import com.example.fruitmarket.dto.ProductVariantDTO;
import com.example.fruitmarket.enums.ImageType;
import com.example.fruitmarket.model.ProductVariant;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VariantService {
    ProductVariant createVariant(Long productId,
                                 ProductVariant variant,
                                 List<MultipartFile> files,
                                 ImageType imageType) throws IOException;

}
