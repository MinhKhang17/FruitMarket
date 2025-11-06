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
//        seedProducts();
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

    private void seedDefaultAdmin() {
        String defaultAdminUsername = "admin";
        if (!userRepository.existsByUsername(defaultAdminUsername)) {
            Users admin = new Users();
            admin.setUsername(defaultAdminUsername);
            admin.setPassword(passwordEncoder.encode("1"));
            admin.setRole("ADMIN");
            admin.setPhone("0900000000");
            admin.setEmail("admin@gmail.com");
            admin.setStatus(UserStatus.ACTIVE);

            Users savedAdmin = userRepository.save(admin);

            User_detail adminDetail = new User_detail();
            adminDetail.setAddress("System HQ");
            adminDetail.setPhone("0900000000");
            adminDetail.setUser(savedAdmin);
            userDetailRepo.save(adminDetail);

            log.info("✅ Seeded default admin (username='admin', password='1')");
        } else {
            log.info("ℹ️ Admin user already exists, skipping");
        }
    }

    // ======================
// 🥭 Seed products + link ảnh static/images
// ======================
//    private void seedProducts() {
//        var savedCategories = categorysRepository.findAll();
//        var savedBrands = brandsRepository.findAll();
//        if (savedCategories.isEmpty() || savedBrands.isEmpty()) {
//            log.warn("No categories or brands found - skipping product seeding");
//            return;
//        }
//        Categorys defaultCategory = savedCategories.get(0);
//        Brands defaultBrand = savedBrands.get(0);
//
//        // Đường dẫn ảnh đúng với src/main/resources/static/images
//        final String IMG_APPLE       = "/images/red_apple.jpg";
//        final String IMG_SPINACH     = "/images/baby_spinach.jpg";
//        final String IMG_DRIED_MANGO = "/images/dried_mango.jpg";
//
//        // Upsert sản phẩm + biến thể (tránh duplicate)
//        upsertProductWithVariant(
//                "Red Apple",
//                "Crisp, sweet red apples - fresh and juicy.",
//                defaultCategory,
//                defaultBrand,
//                Units.KILOGRAM,
//                "1kg",
//                new BigDecimal("49000.00"),
//                100,
//                IMG_APPLE
//        );
//
//        upsertProductWithVariant(
//                "Baby Spinach",
//                "Fresh baby spinach - tender leaves, great for salads.",
//                defaultCategory,
//                defaultBrand,
//                Units.KILOGRAM,
//                "1kg",
//                new BigDecimal("24000.00"),
//                80,
//                IMG_SPINACH
//        );
//
//        upsertProductWithVariant(
//                "Dried Mango",
//                "Sweet dried mango slices - tasty snack.",
//                defaultCategory,
//                defaultBrand,
//                Units.KILOGRAM,
//                "1kg",
//                new BigDecimal("55000.00"),
//                120,
//                IMG_DRIED_MANGO
//        );
//
//        log.info("✅ Product seeding complete (images linked to /static/images)");
//    }

/* ==========================================================
   Helpers
   ========================================================== */

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