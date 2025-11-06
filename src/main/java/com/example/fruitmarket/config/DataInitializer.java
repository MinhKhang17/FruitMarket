package com.example.fruitmarket.config;

import com.example.fruitmarket.controller.GeoJsonLoader;
import com.example.fruitmarket.enums.ProductStatus;
import com.example.fruitmarket.enums.Units;
import com.example.fruitmarket.enums.UserStatus;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategorysRepository categorysRepository;
    private final BrandsRepository brandsRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserDetailRepo userDetailRepo;
    private final ProvinceRepo provinceRepo;
    private final DistrictRepo districtRepo;
    private final WardRepo wardRepo;
    private final GeoJsonLoader geoJsonLoader;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedGeoData();
        seedCategories();
        seedBrands();
        seedDefaultUser();
        seedProducts();
    }

    // ==========================================
    // 🗺️ SEED GEO DATA FROM JSON FILES
    // ==========================================
    private void seedGeoData() {
        if (provinceRepo.count() == 0) {
            log.info("📦 Importing provinces...");
            geoJsonLoader.getProvinces().forEach(p -> {
                Province province = new Province();
                province.setProvinceId(((Number) p.id()).intValue());
                province.setProvinceName(p.name());
                provinceRepo.save(province);
            });
            log.info("✅ Imported provinces");
        }

        if (districtRepo.count() == 0) {
            log.info("📦 Importing districts...");
            geoJsonLoader.getDistricts().forEach(d -> {
                District district = new District();
                district.setDistrictId(d.id());
                district.setDistrictName(d.name());
                if (d.provinceId() != null)
                    provinceRepo.findById(d.provinceId()).ifPresent(district::setProvince);
                districtRepo.save(district);
            });
            log.info("✅ Imported districts");
        }

        if (wardRepo.count() == 0) {
            log.info("📦 Importing wards...");
            geoJsonLoader.getWards().forEach(w -> {
                Ward ward = new Ward();
                ward.setWardCode(w.id());
                ward.setWardName(w.name());
                if (w.districtId() != null)
                    districtRepo.findById(w.districtId()).ifPresent(ward::setDistrict);
                wardRepo.save(ward);
            });
            log.info("✅ Imported wards");
        }
    }

    private void seedCategories() {
        if (categorysRepository.count() == 0) {
            List<Categorys> categories = List.of(
                    createCategory("Fruits", true),
                    createCategory("Vegetables", true),
                    createCategory("Dried Goods", true)
            );
            categorysRepository.saveAll(categories);
            log.info("Seeded {} categories", categories.size());
        } else {
            log.info("Categories already present, skipping");
        }
    }

    private void seedBrands() {
        if (brandsRepository.count() == 0) {
            List<Brands> brands = List.of(
                    createBrand("FreshFarm", true),
                    createBrand("GreenGrow", true),
                    createBrand("NaturePack", true)
            );
            brandsRepository.saveAll(brands);
            log.info("Seeded {} brands", brands.size());
        } else {
            log.info("Brands already present, skipping");
        }
    }

    private void seedDefaultUser() {
        String defaultUsername = "user";
        if (!userRepository.existsByUsername(defaultUsername)) {
            Users u = new Users();
            u.setUsername(defaultUsername);
            u.setPassword(passwordEncoder.encode("password"));
            u.setRole("CLIENT");
            u.setPhone("0933456172");
            u.setEmail("user@gmail.com");
            u.setStatus(UserStatus.ACTIVE);
            Users savedUser = userRepository.save(u);

            User_detail userDetail = new User_detail();
            userDetail.setAddress("1 Mạc Thiên Tích, Pháo Ðài, Hà Tiên");
            userDetail.setPhone("0933567467");
            userDetail.setUser(savedUser);

            // 🗺️ Lấy các entity từ DB (ví dụ: Hà Tiên – tỉnh Kiên Giang)
            Province province = provinceRepo.findById(206)
                    .orElseThrow(() -> new IllegalArgumentException("Province not found"));
            District district = districtRepo.findById(1544)
                    .orElseThrow(() -> new IllegalArgumentException("District not found"));
            Ward ward = wardRepo.findById("520108")
                    .orElseThrow(() -> new IllegalArgumentException("Ward not found"));

            // ✅ Gán vào userDetail
            userDetail.setProvince(province);
            userDetail.setDistrict(district);
            userDetail.setWard(ward);

            userDetailRepo.save(userDetail);


            Users user = new Users();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("password"));
            user.setRole("ADMIN");
            user.setPhone("0933567467");
            user.setStatus(UserStatus.ACTIVE);
            user.setEmail("admin123@gmail.com");
            userRepository.save(user);
            log.info("Seeded default user and userDetail");
        } else {
            log.info("Default user already exists, skipping user seed");
        }
    }

    // ======================
    private void seedProducts() {
        // prepare categories & brands (use first saved ones)
        var savedCategories = categorysRepository.findAll();
        var savedBrands = brandsRepository.findAll();
        if (savedCategories.isEmpty() || savedBrands.isEmpty()) {
            log.warn("No categories or brands found - skipping product seeding");
            return;
        }
        Categorys defaultCategory = savedCategories.get(0);
        Brands defaultBrand = savedBrands.get(0);

        // helper để tạo product nhanh
        BiConsumer<Product, String> setImageIfPossible = (product, imagePath) -> {
            try {
                // set image on product if setter exists
                product.getClass().getMethod("setImageUrl", String.class).invoke(product, imagePath);
            } catch (NoSuchMethodException ignored) {
                // entity does not have setImageUrl - ignore
            } catch (Exception e) {
                log.warn("Failed to set product image via reflection: {}", e.getMessage());
            }
        };

        // ---- Red Apple ----
        if (!productRepository.existsByProductName("Red Apple")) {
            Product p1 = new Product();
            p1.setProductName("Red Apple");
            p1.setProduct_description("Crisp, sweet red apples - fresh and juicy.");
            p1.setCategory(defaultCategory);
            p1.setBrand(defaultBrand);
            p1.setStatus(ProductStatus.ACTIVE);
            p1.setUnit(Units.KILOGRAM);

            setImageIfPossible.accept(p1, "/images/red_apple.jpg");

            ProductVariant v11 = new ProductVariant();
            v11.setVariant_name("1kg");
            v11.setPrice(new BigDecimal("49000.00"));
            v11.setProduct(p1);
            v11.setStock(100);
            v11.setImage(new Image());
            Image image = new Image();
            image.setUrl("/images/red_apple.jpg");
            v11.setImage(image);
            v11.setStatus(ProductStatus.ACTIVE);
            try {
                v11.getClass().getMethod("setImageUrl", String.class).invoke(v11, "/images/red_apple.jpg");
            } catch (Exception ignored) {
            }

            p1.getVariants().add(v11);

            productRepository.save(p1);
            log.info("Seeded product Red Apple with variants");
        } else {
            log.info("Product 'Red Apple' exists, skipping");
        }

        // ---- Baby Spinach ----
        if (!productRepository.existsByProductName("Baby Spinach")) {
            Product p2 = new Product();
            p2.setProductName("Baby Spinach");
            p2.setProduct_description("Fresh baby spinach - tender leaves, great for salads.");
            p2.setCategory(defaultCategory);
            p2.setBrand(defaultBrand);
            p2.setStatus(ProductStatus.ACTIVE);
            p2.setUnit(Units.KILOGRAM);

            setImageIfPossible.accept(p2, "/images/baby_spinach.jpg");

            ProductVariant s21 = new ProductVariant();
            s21.setVariant_name("1kg");
            s21.setPrice(new BigDecimal("24000.00"));
            s21.setProduct(p2);
            s21.setStock(80);
            s21.setImage(new Image());
            Image image1 = new Image();
            image1.setUrl("/images/baby_spinach.jpg");
            s21.setImage(image1);
            s21.setStatus(ProductStatus.ACTIVE);
            try {
                s21.getClass().getMethod("setImageUrl", String.class).invoke(s21, "/images/baby_spinach.jpg");
            } catch (Exception ignored) {
            }

            p2.getVariants().add(s21);

            productRepository.save(p2);
            log.info("Seeded product Baby Spinach with variants");
        } else {
            log.info("Product 'Baby Spinach' exists, skipping");
        }

        // ---- Dried Mango ----
        if (!productRepository.existsByProductName("Dried Mango")) {
            Product p3 = new Product();
            p3.setProductName("Dried Mango");
            p3.setProduct_description("Sweet dried mango slices - tasty snack.");
            p3.setCategory(defaultCategory);
            p3.setBrand(defaultBrand);
            p3.setStatus(ProductStatus.ACTIVE);
            p3.setUnit(Units.KILOGRAM);
            setImageIfPossible.accept(p3, "/images/dried_mango.jpg");

            ProductVariant m31 = new ProductVariant();
            m31.setVariant_name("1kg");
            m31.setPrice(new BigDecimal("55000.00"));
            m31.setProduct(p3);
            m31.setStock(120);
            m31.setImage(new Image());
            Image image2 = new Image();
            image2.setUrl("/images/dried_mango.jpg");
            m31.setImage(image2);
            m31.setStatus(ProductStatus.ACTIVE);
            try {
                m31.getClass().getMethod("setImageUrl", String.class).invoke(m31, "/images/dried_mango.jpg");
            } catch (Exception ignored) {
            }

            p3.getVariants().add(m31);

            productRepository.save(p3);
            log.info("Seeded product Dried Mango with variants");
        } else {
            log.info("Product 'Dried Mango' exists, skipping");
        }

        // NOTE: do not link placeholder.png except as fallback in UI/templates
        log.info("Product seeding complete");
    }
    /** Tạo/cập nhật product + 1 variant theo tên (idempotent, không trùng). */
    private void upsertProductWithVariant(
            String productName,
            String description,
            Categorys category,
            Brands brand,
            Units unit,
            String variantName,
            BigDecimal price,
            int stock,
            String imagePath
    ) {
        Product product = productRepository.findByProductName(productName);
        if (product == null) {
            product = new Product();
            product.setProductName(productName);
            product.setProduct_description(description);
            product.setCategory(category);
            product.setBrand(brand);
            product.setStatus(com.example.fruitmarket.enums.ProductStatus.ACTIVE);
            product.setUnit(unit);
            trySetImageUrl(product, imagePath); // nếu Product có setImageUrl(String)

            // chuẩn bị list variants nếu null
            if (product.getVariants() == null) {
                product.setVariants(new java.util.ArrayList<>());
            }
        }

        // kiểm tra variant theo tên trong product
        ProductVariant variant = findVariantByName(product, variantName);
        if (variant == null) {
            variant = new ProductVariant();
            variant.setVariant_name(variantName);
            variant.setProduct(product); // 🔴 bắt buộc
            product.getVariants().add(variant);
        }

        // cập nhật dữ liệu variant (safe cập nhật nếu đã tồn tại)
        variant.setPrice(price);
        variant.setStock(stock);
        variant.setStatus(com.example.fruitmarket.enums.ProductStatus.ACTIVE);

        // Link ảnh cho variant:
        // - Nếu entity có setImageUrl(String) → dùng trực tiếp
        // - Ngược lại nếu có quan hệ Image → đảm bảo chỉ tạo 1 Image và set url
        boolean usedSetter = trySetImageUrl(variant, imagePath);
        if (!usedSetter) {
            if (variant.getImage() == null) {
                Image img = new Image();
                img.setUrl(imagePath);
                variant.setImage(img); // cascade từ variant
            } else {
                variant.getImage().setUrl(imagePath);
            }
        }

        // Lưu: chỉ cần save(product) (Cascade.ALL với variants) là đủ
        productRepository.save(product);
        log.info("Upserted product '{}' with variant '{}'", productName, variantName);
    }

    /** Tìm variant theo tên trong 1 product (không cần repo riêng). */
    private ProductVariant findVariantByName(Product product, String variantName) {
        if (product.getVariants() == null) return null;
        for (ProductVariant v : product.getVariants()) {
            if (v.getVariant_name() != null && v.getVariant_name().equalsIgnoreCase(variantName)) {
                return v;
            }
        }
        return null;
    }

    /** Thử gọi setImageUrl(String) nếu entity có, trả true nếu gọi được. */
    private boolean trySetImageUrl(Object target, String url) {
        try {
            var m = target.getClass().getMethod("setImageUrl", String.class);
            m.invoke(target, url);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Exception e) {
            log.warn("Failed to call setImageUrl on {}: {}", target.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }


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
}