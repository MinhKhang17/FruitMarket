package com.example.fruitmarket.config;

import com.example.fruitmarket.Enums.ImageType;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategorysRepository categorysRepository;
    private final BrandsRepository brandsRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedCategoriesAndBrands();
        seedUser();
        seedProductsAndVariants();
    }

    // =======================================
    // 1️⃣ Seed Category & Brand
    // =======================================
    private void seedCategoriesAndBrands() {
        if (categorysRepository.count() == 0) {
            List<Categorys> categories = List.of(
                    createCategory("Fruits", true),
                    createCategory("Vegetables", true),
                    createCategory("Dried Goods", true)
            );
            categorysRepository.saveAll(categories);
            log.info("✅ Seeded {} categories", categories.size());
        }

        if (brandsRepository.count() == 0) {
            List<Brands> brands = List.of(
                    createBrand("FreshFarm", true),
                    createBrand("GreenGrow", true),
                    createBrand("NaturePack", true)
            );
            brandsRepository.saveAll(brands);
            log.info("✅ Seeded {} brands", brands.size());
        }
    }

    // =======================================
    // 2️⃣ Seed User demo
    // =======================================
    private void seedUser() {
        if (userRepository.count() == 0) {
            Users user = new Users();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("password"));
            user.setRole("CLIENT");
            user.setPhone("0933456172");
            user.setEmail("user@gmail.com");
            user.setStatus("ACTIVE");
            userRepository.save(user);
            log.info("✅ Seeded demo user");
        }
    }

    // =======================================
    // 3️⃣ Seed Product + Variants + Images
    // =======================================
    private void seedProductsAndVariants() {
        if (productRepository.count() > 0) {
            log.info("ℹ️ Products already exist — skip seeding");
            return;
        }

        List<Categorys> categories = categorysRepository.findAll();
        List<Brands> brands = brandsRepository.findAll();

        if (categories.isEmpty() || brands.isEmpty()) {
            log.warn("⚠️ Skipping product seeding: categories or brands not found.");
            return;
        }

        Categorys c1 = categories.get(0);
        Categorys c2 = categories.size() > 1 ? categories.get(1) : c1;
        Brands b1 = brands.get(0);
        Brands b2 = brands.size() > 1 ? brands.get(1) : b1;

        // --- Create products ---
        Product p1 = createProduct("Red Apple", "Crisp, sweet red apples - base product", c1, b1);
        Product p2 = createProduct("Baby Spinach", "Fresh baby spinach - base product", c2, b2);
        Product p3 = createProduct("Dried Mango", "Sweet dried mango slices - base product",
                categories.get(categories.size() - 1), b1);

        // --- Add images (local) ---
        attachLocalImage(p1, "/images/red_apple.jpg");
        attachLocalImage(p2, "/images/baby_spinach.jpg");
        attachLocalImage(p3, "/images/dried_mango.jpg");

        // --- Save all products (cascade sẽ tự save ảnh) ---
        productRepository.saveAll(List.of(p1, p2, p3));

        // --- Create variants ---
        createVariant(p1, "1kg", new BigDecimal("49000.00"));
        createVariant(p1, "500g", new BigDecimal("25000.00"));
        createVariant(p2, "200g", new BigDecimal("29000.00"));
        createVariant(p3, "250g", new BigDecimal("79000.00"));

        log.info("✅ Seeded demo products, variants, and images successfully");
    }

    // =======================================
    // 4️⃣ Helper Methods
    // =======================================
    private Categorys createCategory(String name, boolean status) {
        Categorys c = new Categorys();
        c.setName(name);
        c.setStatus(status);
        return c;
    }

    private Brands createBrand(String name, boolean status) {
        Brands b = new Brands();
        b.setName(name);
        b.setStatus(status);
        return b;
    }

    private Product createProduct(String name, String desc, Categorys c, Brands b) {
        Product p = new Product();
        p.setProduct_name(name);
        p.setProduct_description(desc);
        p.setCategory(c);
        p.setBrand(b);
        p.setImages(new ArrayList<>());
        return p;
    }

    private void attachLocalImage(Product product, String url) {
        Image image = new Image();
        image.setUrl(url);
        image.setImageType(ImageType.PRODUCT);
        product.getImages().add(image); // Không save thủ công → cascade tự xử lý
    }

    private void createVariant(Product product, String name, BigDecimal price) {
        ProductVariant variant = new ProductVariant();
        variant.setVariant_name(name);
        variant.setPrice(price);
        variant.setStock(100);
        variant.setProduct(product);

        // Ảnh variant dùng chung ảnh sp hoặc placeholder
        Image variantImg = new Image();
        variantImg.setUrl(product.getImages().isEmpty() ? "/images/placeholder.png"
                : product.getImages().get(0).getUrl());
        variantImg.setImageType(ImageType.PRODUCT_VARIANT);

        variant.setImage(variantImg); // cascade tự save
        productVariantRepository.save(variant);
    }
}
