package com.example.fruitmarket.config;

import com.example.fruitmarket.controller.GeoJsonLoader;
import com.example.fruitmarket.enums.Units;
import com.example.fruitmarket.enums.UserStatus;
import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
                    createCategory("Trái cây tươi", true),
                    createCategory("Trái cây nhập khẩu", true),
                    createCategory("Trái cây nhiệt đới", true),
                    createCategory("Giỏ quà trái cây", true),
                    createCategory("Trái cây sấy & hạt", true),
                    createCategory("Nước ép trái cây", true),

                    // Bổ sung thêm (vẫn đúng chủ đề trái cây)
                    createCategory("Trái cây theo mùa", true),
                    createCategory("Trái cây hữu cơ", true),
                    createCategory("Trái cây cắt sẵn", true),
                    createCategory("Combo tiết kiệm", true),
                    createCategory("Đặc sản vùng miền", true),
                    createCategory("Mứt & sốt trái cây", true)
            );
            categorysRepository.saveAll(categories);
            log.info("Đã seed {} danh mục (chủ đề trái cây)", categories.size());
        } else {
            log.info("Danh mục đã tồn tại, bỏ qua");
        }
    }

    private void seedBrands() {
        if (brandsRepository.count() == 0) {
            List<Brands> brands = List.of(
                    createBrand("Vườn Nhà", true),
                    createBrand("Miền Trái Cây", true),
                    createBrand("Orchard Việt", true),
                    createBrand("Tropical Garden", true),
                    createBrand("FruitBox", true),
                    createBrand("EcoFarm", true),
                    createBrand("Nông Sản Xanh", true),
                    createBrand("Vina Orchard", true),

                    // Bổ sung thêm
                    createBrand("Sapa Fruit", true),
                    createBrand("Mekong Orchard", true),
                    createBrand("Đà Lạt Farm", true),
                    createBrand("SunFruit", true),
                    createBrand("Happy Orchard", true),
                    createBrand("Highland Fruits", true),
                    createBrand("Organic Valley VN", true),
                    createBrand("FreshHub", true),
                    createBrand("An Nhiên Garden", true),
                    createBrand("Nhà Vườn 68", true)
            );
            brandsRepository.saveAll(brands);
            log.info("Đã seed {} thương hiệu (trái cây)", brands.size());
        } else {
            log.info("Thương hiệu đã tồn tại, bỏ qua");
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
        var savedCategories = categorysRepository.findAll();
        var savedBrands = brandsRepository.findAll();
        if (savedCategories.isEmpty() || savedBrands.isEmpty()) {
            log.warn("Chưa có danh mục hoặc thương hiệu - bỏ qua seed sản phẩm");
            return;
        }

        // Units mặc định theo danh mục trái cây
        java.util.function.Function<String, Units> unitForCategory = cate -> {
            if (cate == null) return Units.PIECE;
            switch (cate) {
                case "Trái cây tươi":
                case "Trái cây nhập khẩu":
                case "Trái cây nhiệt đới":
                case "Trái cây theo mùa":
                case "Trái cây hữu cơ":
                    return Units.KILOGRAM;
                case "Giỏ quà trái cây":
                case "Combo tiết kiệm":
                    return Units.BASKET;
                case "Trái cây sấy & hạt":
                    return Units.KILOGRAM;
                case "Nước ép trái cây":
                case "Trái cây cắt sẵn":
                case "Mứt & sốt trái cây":
                    return Units.PIECE;
                case "Đặc sản vùng miền":
                    return Units.KILOGRAM;
                default:
                    return Units.PIECE;
            }
        };

        // Tên variant mặc định theo đơn vị
        java.util.function.Function<Units, String> variantNameOf = u -> {
            switch (u) {
                case KILOGRAM: return "1kg";
                case PIECE:    return "1 cái";
                case BASKET:   return "Giỏ";
                default:       return "Mặc định";
            }
        };

        // DTO ngắn gọn
        class Row {
            String name, desc, price, image; Units unit; String variant; int stock;
            Row(String n, String d, String p, String img) { this(n,d,p,img,null,null,100); }
            Row(String n, String d, String p, String img, Units u) { this(n,d,p,img,u,null,100); }
            Row(String n, String d, String p, String img, Units u, String v, int s) {
                name=n; desc=d; price=p; image=img; unit=u; variant=v; stock=s;
            }
        }

        // ===== Dataset theo CHỦ ĐỀ TRÁI CÂY =====
        java.util.Map<String, java.util.List<Row>> data = new java.util.HashMap<>();

        // Trái cây tươi (10)
        data.put("Trái cây tươi", java.util.List.of(
                new Row("Táo đỏ", "Táo đỏ giòn ngọt, mọng nước.", "49000.00", "/images/red_apple.jpg", Units.KILOGRAM),
                new Row("Táo xanh", "Táo xanh chua dịu, giòn.", "52000.00", "/images/green_apple.jpg", Units.KILOGRAM),
                new Row("Chuối Cavendish", "Chuối chín thơm, làm sinh tố ngon.", "32000.00", "/images/banana.jpg", Units.KILOGRAM),
                new Row("Cam vàng", "Giàu vitamin C, mọng nước.", "45000.00", "/images/orange.jpg", Units.KILOGRAM),
                new Row("Nho không hạt xanh", "Giòn ngọt, dễ ăn.", "78000.00", "/images/grapes.jpg", Units.KILOGRAM),
                new Row("Lê Hàn Quốc", "Ngọt mát, nhiều nước.", "89000.00", "/images/pear.jpg", Units.KILOGRAM),
                new Row("Dưa lưới", "Thơm, thịt dày, ngọt.", "95000.00", "/images/melon.jpg", Units.PIECE, "1 trái ~1.2–1.6kg", 60),
                new Row("Thanh long ruột đỏ", "Ngọt dịu, giàu chất xơ.", "65000.00", "/images/dragon_fruit.jpg", Units.KILOGRAM),
                new Row("Bưởi da xanh", "Múi to, ít hạt.", "69000.00", "/images/pomelo.jpg", Units.PIECE, "1 trái ~1.2–1.8kg", 70),
                new Row("Dưa hấu", "Ngọt mát, giải khát.", "89000.00", "/images/watermelon.jpg", Units.PIECE, "1 trái ~3–4kg", 40)
        ));

        // Trái cây nhập khẩu (10)
        data.put("Trái cây nhập khẩu", java.util.List.of(
                new Row("Việt quất (Blueberry)", "Trái mọng giàu chất chống oxy hoá.", "320000.00", "/images/blueberry.jpg", Units.KILOGRAM),
                new Row("Dâu tây", "Tươi mọng, làm bánh/sinh tố.", "120000.00", "/images/strawberry.jpg", Units.KILOGRAM),
                new Row("Kiwi vàng", "Ngọt dịu, thơm.", "145000.00", "/images/kiwi.jpg", Units.KILOGRAM),
                new Row("Nho đen không hạt", "Đậm vị, thơm.", "98000.00", "/images/grapes_black.jpg", Units.KILOGRAM),
                new Row("Lựu", "Hạt đỏ, giòn ngọt.", "150000.00", "/images/pomegranate.jpg", Units.KILOGRAM),
                new Row("Lê Nam Phi", "Giòn mát, vị thanh.", "78000.00", "/images/pear.jpg", Units.KILOGRAM),
                new Row("Táo Envy", "Giòn, ngọt cao cấp.", "99000.00", "/images/red_apple.jpg", Units.KILOGRAM),
                new Row("Cam Cara", "Thịt nhoé hồng, ngọt thơm.", "68000.00", "/images/orange.jpg", Units.KILOGRAM),
                new Row("Cherry", "Thịt giòn, vị ngọt đậm.", "450000.00", "/images/cherry.jpg", Units.KILOGRAM),
                new Row("Nectarine", "Giòn ngọt, thơm nhẹ.", "165000.00", "/images/nectarine.jpg", Units.KILOGRAM)
        ));

        // Trái cây nhiệt đới (10)
        data.put("Trái cây nhiệt đới", java.util.List.of(
                new Row("Xoài cát", "Ngọt thơm, dẻo.", "69000.00", "/images/mango_fresh.jpg", Units.KILOGRAM),
                new Row("Dứa (thơm)", "Thơm lừng, vàng ruộm.", "39000.00", "/images/pineapple.jpg", Units.PIECE, "1 trái", 100),
                new Row("Đu đủ", "Mềm, ngọt, nhiều vitamin.", "35000.00", "/images/papaya.jpg", Units.PIECE, "1 trái ~1kg", 100),
                new Row("Chôm chôm", "Múi dày, ngọt nước.", "65000.00", "/images/rambutan.jpg", Units.KILOGRAM),
                new Row("Măng cụt", "Nữ hoàng trái cây, ngọt thanh.", "145000.00", "/images/mangosteen.jpg", Units.KILOGRAM),
                new Row("Sầu riêng", "Béo ngậy, thơm mạnh.", "180000.00", "/images/durian.jpg", Units.KILOGRAM),
                new Row("Vú sữa", "Thịt mềm, ngọt sữa.", "98000.00", "/images/star_apple.jpg", Units.KILOGRAM),
                new Row("Ổi ruột hồng", "Giòn, thơm, ít hạt.", "45000.00", "/images/guava.jpg", Units.KILOGRAM),
                new Row("Dâu tằm", "Chua ngọt, lạ miệng.", "120000.00", "/images/mulberry.jpg", Units.KILOGRAM),
                new Row("Cóc", "Chua giòn, chấm mắm đường.", "39000.00", "/images/ambarella.jpg", Units.KILOGRAM)
        ));

        // Giỏ quà trái cây (10) — dùng Units.BASKET
        data.put("Giỏ quà trái cây", java.util.List.of(
                new Row("Giỏ trái cây tiêu chuẩn", "Giỏ mix 3–4 loại theo mùa.", "289000.00", "/images/fruit_basket_standard.jpg", Units.BASKET, "Giỏ tiêu chuẩn", 30),
                new Row("Giỏ premium", "Giỏ trái cây cao cấp (nhập khẩu).", "489000.00", "/images/fruit_basket_premium.jpg", Units.BASKET, "Giỏ premium", 25),
                new Row("Giỏ sức khoẻ", "Trái cây giàu vitamin & chất xơ.", "349000.00", "/images/fruit_basket_health.jpg", Units.BASKET, "Giỏ sức khoẻ", 25),
                new Row("Giỏ cho mẹ & bé", "Trái cây mềm, dễ ăn.", "329000.00", "/images/fruit_basket_health.jpg", Units.BASKET, "Giỏ mẹ & bé", 20),
                new Row("Giỏ cảm ơn", "Quà tặng tri ân trang nhã.", "399000.00", "/images/fruit_basket_thanks.jpg", Units.BASKET, "Giỏ cảm ơn", 20),
                new Row("Giỏ Tết nhỏ", "Giỏ ấm cúng ngày Tết.", "459000.00", "/images/fruit_basket_tet_small.jpg", Units.BASKET, "Giỏ Tết S", 20),
                new Row("Giỏ Tết lớn", "Giỏ đầy đặn, nhiều loại cao cấp.", "789000.00", "/images/fruit_basket_tet_large.jpg", Units.BASKET, "Giỏ Tết L", 15),
                new Row("Hộp quà mini", "Hộp 2–3 loại, gọn nhẹ.", "189000.00", "/images/fruit_basket_tet_small.jpg", Units.BASKET, "Hộp mini", 40),
                new Row("Hộp quà doanh nghiệp", "Set quà trái cây nhập khẩu.", "989000.00", "/images/fruit_box_corporate.jpg", Units.BASKET, "Hộp DN", 10),
                new Row("Giỏ sinh nhật", "Trang trí nơ, thiệp chúc mừng.", "469000.00", "/images/fruit_basket_birthday.jpg", Units.BASKET, "Giỏ sinh nhật", 25)
        ));

        // Trái cây sấy & hạt (10)
        data.put("Trái cây sấy & hạt", java.util.List.of(
                new Row("Xoài sấy dẻo", "Vị ngọt tự nhiên.", "55000.00", "/images/dried_mango.jpg", Units.KILOGRAM, "Gói 200g", 120),
                new Row("Chuối sấy", "Giòn rụm, ít dầu.", "39000.00", "/images/dried_banana.jpg", Units.KILOGRAM, "Gói 150g", 140),
                new Row("Dứa sấy", "Chua ngọt, thơm.", "45000.00", "/images/dried_pineapple.jpg", Units.KILOGRAM, "Gói 150g", 140),
                new Row("Táo sấy", "Miếng mỏng, giòn nhẹ.", "59000.00", "/images/dried_apple.jpg", Units.KILOGRAM, "Gói 150g", 120),
                new Row("Mít sấy", "Giòn ngọt, thơm đặc trưng.", "65000.00", "/images/dried_jackfruit.jpg", Units.KILOGRAM, "Gói 200g", 120),
                new Row("Hạnh nhân", "Giòn béo, cao cấp.", "185000.00", "/images/almonds.jpg", Units.KILOGRAM, "Gói 250g", 100),
                new Row("Hạt điều", "Điều rang muối.", "210000.00", "/images/cashews.jpg", Units.KILOGRAM, "Gói 250g", 100),
                new Row("Óc chó", "Giàu Omega-3.", "195000.00", "/images/walnuts.jpg", Units.KILOGRAM, "Gói 250g", 90),
                new Row("Nam việt quất sấy", "Cranberry chua ngọt.", "175000.00", "/images/dried_cranberry.jpg", Units.KILOGRAM, "Gói 200g", 100),
                new Row("Nho khô", "Ngọt tự nhiên.", "48000.00", "/images/raisins.jpg", Units.KILOGRAM, "Gói 200g", 140)
        ));

        // Nước ép trái cây (10)
        data.put("Nước ép trái cây", java.util.List.of(
                new Row("Nước cam ép", "Ép tươi mỗi ngày.", "39000.00", "/images/juice_orange.jpg", Units.PIECE, "Chai 350ml", 120),
                new Row("Nước táo ép", "Vị ngọt thanh.", "39000.00", "/images/juice_apple.jpg", Units.PIECE, "Chai 350ml", 120),
                new Row("Nước dứa ép", "Thơm dịu, mát lành.", "35000.00", "/images/juice_pineapple.jpg", Units.PIECE, "Chai 350ml", 120),
                new Row("Nước ổi ép", "Ngon lạ, nhiều vitamin C.", "35000.00", "/images/juice_guava.jpg", Units.PIECE, "Chai 350ml", 120),
                new Row("Nước dưa hấu ép", "Giải nhiệt nhanh.", "32000.00", "/images/juice_watermelon.jpg", Units.PIECE, "Chai 350ml", 120),
                new Row("Nước xoài ép", "Đặc sánh, thơm.", "42000.00", "/images/juice_mango.jpg", Units.PIECE, "Chai 350ml", 100),
                new Row("Nước chanh dây", "Chua ngọt kích thích vị giác.", "32000.00", "/images/juice_passion.jpg", Units.PIECE, "Chai 350ml", 120),
                new Row("Nước dâu tây", "Hồng nhạt, thơm dâu.", "45000.00", "/images/juice_strawberry.jpg", Units.PIECE, "Chai 350ml", 100),
                new Row("Nước việt quất", "Màu tím đẹp mắt.", "52000.00", "/images/juice_blueberry.jpg", Units.PIECE, "Chai 350ml", 90),
                new Row("Nước táo – cần tây", "Detox nhẹ, tốt cho dáng.", "49000.00", "/images/juice_apple_celery.jpg", Units.PIECE, "Chai 350ml", 90)
        ));

        data.put("Trái cây theo mùa", java.util.List.of(
                new Row("Vải thiều", "Vải thiều chín tự nhiên, ngọt thanh.", "78000.00", "/images/lychee.jpg", Units.KILOGRAM),
                new Row("Nhãn lồng", "Nhãn cùi dày, thơm ngọt.", "72000.00", "/images/longan.jpg", Units.KILOGRAM),
                new Row("Mận hậu", "Mận Mộc Châu giòn ngọt.", "69000.00", "/images/plum.jpg", Units.KILOGRAM),
                new Row("Quýt đường", "Vỏ mỏng, vị ngọt mát.", "52000.00", "/images/mandarin.jpg", Units.KILOGRAM),
                new Row("Cam sành", "Ngọt đậm, mọng nước.", "48000.00", "/images/orange.jpg", Units.KILOGRAM),
                new Row("Xoài keo", "Thơm dẻo, ít xơ.", "65000.00", "/images/mango_keo.jpg", Units.KILOGRAM),
                new Row("Ổi lê", "Giòn, ít hạt.", "45000.00", "/images/guava_pear.jpg", Units.KILOGRAM),
                new Row("Dâu tằm", "Chua ngọt lạ miệng.", "120000.00", "/images/mulberry.jpg", Units.KILOGRAM),
                new Row("Cóc xanh", "Chua giòn, ăn vặt mê ly.", "39000.00", "/images/ambarella.jpg", Units.KILOGRAM),
                new Row("Bưởi Năm Roi", "Múi to, ít hạt.", "69000.00", "/images/pomelo.jpg", Units.PIECE, "1 trái ~1.2–1.6kg", 70)
        ));

        data.put("Trái cây hữu cơ", java.util.List.of(
                new Row("Táo hữu cơ", "Táo canh tác hữu cơ, an toàn.", "99000.00", "/images/red_apple.jpg", Units.KILOGRAM),
                new Row("Chuối hữu cơ", "Chuối chín tự nhiên, sạch.", "52000.00", "/images/banana.jpg", Units.KILOGRAM),
                new Row("Cam hữu cơ", "Vị ngọt thanh, không dư lượng.", "78000.00", "/images/orange.jpg", Units.KILOGRAM),
                new Row("Bơ hữu cơ", "Bơ béo, thịt dẻo.", "125000.00", "/images/organic_avocado.jpg", Units.KILOGRAM),
                new Row("Dâu tây hữu cơ", "Mọng nước, ít thuốc BVTV.", "185000.00", "/images/organic_strawberry.jpg", Units.KILOGRAM),
                new Row("Việt quất hữu cơ", "Giàu chất chống oxy hoá.", "350000.00", "/images/organic_blueberry.jpg", Units.KILOGRAM),
                new Row("Xoài hữu cơ", "Thơm dẻo, đạt chuẩn hữu cơ.", "115000.00", "/images/organic_mango.jpg", Units.KILOGRAM),
                new Row("Lê hữu cơ", "Vị mát, nhiều nước.", "98000.00", "/images/pear.jpg", Units.KILOGRAM),
                new Row("Kiwi hữu cơ", "Chua dịu, giàu vitamin C.", "165000.00", "/images/organic_kiwi.jpg", Units.KILOGRAM),
                new Row("Bưởi hữu cơ", "Vỏ sáng, múi nhiều.", "98000.00", "/images/pomelo.jpg", Units.PIECE, "1 trái", 60)
        ));

        data.put("Trái cây cắt sẵn", java.util.List.of(
                new Row("Hộp dưa hấu cắt", "Dưa hấu cắt miếng, ướp lạnh.", "39000.00", "/images/cup_watermelon.jpg", Units.PIECE, "Hộp 300g", 120),
                new Row("Hộp dứa cắt", "Dứa cắt sẵn tiện lợi.", "39000.00", "/images/cup_pineapple.jpg", Units.PIECE, "Hộp 250g", 120),
                new Row("Hộp xoài cắt", "Xoài chín thơm dẻo.", "45000.00", "/images/cup_mango.jpg", Units.PIECE, "Hộp 250g", 100),
                new Row("Hộp thanh long cắt", "Thanh long ruột đỏ mát lạnh.", "39000.00", "/images/cup_dragonfruit.jpg", Units.PIECE, "Hộp 300g", 120),
                new Row("Hộp dưa lưới cắt", "Ngọt dịu, thơm.", "49000.00", "/images/cup_melon.jpg", Units.PIECE, "Hộp 250g", 100),
                new Row("Hộp đu đủ cắt", "Mềm ngọt, dễ ăn.", "35000.00", "/images/cup_watermelon.jpg", Units.PIECE, "Hộp 250g", 120),
                new Row("Hộp mix 3 loại", "Dưa hấu + dứa + xoài.", "52000.00", "/images/cup_melon.jpg", Units.PIECE, "Hộp 300g", 140),
                new Row("Hộp mix 5 loại", "Dưa lưới + táo + ổi + thanh long + dứa.", "69000.00", "/images/cup_melon.jpg", Units.PIECE, "Hộp 350g", 120),
                new Row("Hộp nho tách cuống", "Nho rửa sạch, tách cuống.", "59000.00", "/images/cup_grapes.jpg", Units.PIECE, "Hộp 200g", 120),
                new Row("Hộp dâu tây cắt", "Dâu cắt lát, tráng miệng.", "69000.00", "/images/cup_strawberry.jpg", Units.PIECE, "Hộp 200g", 100)
        ));

        data.put("Combo tiết kiệm", java.util.List.of(
                new Row("Combo sáng khoẻ", "Chuối + táo + cam (dành cho bữa sáng).", "119000.00", "/images/combo_breakfast.jpg", Units.BASKET, "Giỏ nhỏ ~1.5kg", 50),
                new Row("Combo vitamin C", "Cam + dứa + ổi giàu vitamin C.", "139000.00", "/images/combo_vitc.jpg", Units.BASKET, "Giỏ ~2kg", 45),
                new Row("Combo giải nhiệt", "Dưa hấu + dừa + cam.", "169000.00", "/images/combo_cool.jpg", Units.BASKET, "Giỏ ~3kg", 40),
                new Row("Combo tốt cho dáng", "Táo + bưởi + dưa leo (ăn kèm).", "159000.00", "/images/combo_shape.jpg", Units.BASKET, "Giỏ ~2.5kg", 40),
                new Row("Combo gia đình", "Xoài + cam + chuối + táo.", "199000.00", "/images/combo_family.jpg", Units.BASKET, "Giỏ ~4kg", 35),
                new Row("Combo cho bé", "Dâu + việt quất + chuối.", "189000.00", "/images/combo_kid.jpg", Units.BASKET, "Giỏ ~1.5kg", 35),
                new Row("Combo văn phòng", "Táo + nho + lê (dễ ăn).", "179000.00", "/images/combo_office.jpg", Units.BASKET, "Giỏ ~2.5kg", 40),
                new Row("Combo cuối tuần", "Dưa lưới + cam + dứa.", "209000.00", "/images/combo_weekend.jpg", Units.BASKET, "Giỏ ~3kg", 30),
                new Row("Combo detox", "Táo + cần tây + chanh (nguyên liệu).", "149000.00", "/images/combo_detox.jpg", Units.BASKET, "Giỏ ~2kg", 40),
                new Row("Combo nhập khẩu mini", "Táo Envy + nho đen + kiwi.", "259000.00", "/images/combo_import.jpg", Units.BASKET, "Giỏ ~1.8kg", 25)
        ));

        data.put("Đặc sản vùng miền", java.util.List.of(
                new Row("Vải thiều Bắc Giang", "Thơm ngọt danh tiếng.", "78000.00", "/images/lychee_bg.jpg", Units.KILOGRAM),
                new Row("Hồng giòn Đà Lạt", "Giòn tan, ít chát.", "98000.00", "/images/persimmon_dl.jpg", Units.KILOGRAM),
                new Row("Cam Cao Phong", "Vị ngọt thanh mát.", "62000.00", "/images/orange.jpg", Units.KILOGRAM),
                new Row("Mận Mộc Châu", "Giòn ngọt, thơm.", "69000.00", "/images/plum_mc.jpg", Units.KILOGRAM),
                new Row("Bưởi da xanh Bến Tre", "Múi nhiều, ít hạt.", "79000.00", "/images/pomelo_bt.jpg", Units.PIECE, "1 trái", 60),
                new Row("Sầu riêng Ri6", "Cơm vàng béo ngậy.", "195000.00", "/images/durian_ri6.jpg", Units.KILOGRAM),
                new Row("Chôm chôm Long Khánh", "Múi dày, ngọt.", "65000.00", "/images/rambutan_lk.jpg", Units.KILOGRAM),
                new Row("Quýt hồng Lai Vung", "Đậm vị, mọng nước.", "85000.00", "/images/mandarin_lv.jpg", Units.KILOGRAM),
                new Row("Thanh trà Huế", "Thơm nhẹ, vị thanh.", "98000.00", "/images/thanh_tra_hue.jpg", Units.KILOGRAM),
                new Row("Dừa xiêm Bến Tre", "Nước ngọt, mát.", "25000.00", "/images/coconut_bt.jpg", Units.PIECE, "1 trái", 100)
        ));

        data.put("Mứt & sốt trái cây", java.util.List.of(
                new Row("Mứt dâu", "Mứt dâu thơm ngọt, phết bánh mì.", "52000.00", "/images/jam_strawberry.jpg", Units.PIECE, "Hũ 250g", 160),
                new Row("Mứt việt quất", "Mứt blueberry vị đậm đà.", "65000.00", "/images/jam_blueberry.jpg", Units.PIECE, "Hũ 250g", 150),
                new Row("Mứt cam", "Vỏ cam cắt sợi, thơm dịu.", "54000.00", "/images/jam_orange.jpg", Units.PIECE, "Hũ 250g", 150),
                new Row("Mứt dứa", "Vị chua ngọt, vàng đẹp.", "49000.00", "/images/jam_pineapple.jpg", Units.PIECE, "Hũ 250g", 150),
                new Row("Sốt xoài", "Xốt xoài làm tráng miệng.", "59000.00", "/images/sauce_mango.jpg", Units.PIECE, "Chai 300ml", 140),
                new Row("Sốt dâu tây", "Rưới kem, pancake, waffle.", "65000.00", "/images/sauce_strawberry.jpg", Units.PIECE, "Chai 300ml", 140),
                new Row("Sốt phúc bồn tử", "Raspberry chua nhẹ, sang vị.", "69000.00", "/images/sauce_raspberry.jpg", Units.PIECE, "Chai 300ml", 130),
                new Row("Mứt táo quế", "Táo & quế ấm áp.", "62000.00", "/images/jam_apple_cinnamon.jpg", Units.PIECE, "Hũ 250g", 120),
                new Row("Compote trái cây", "Trái cây nấu mềm, ít đường.", "59000.00", "/images/compote_mixed.jpg", Units.PIECE, "Hũ 300g", 120),
                new Row("Puree xoài", "Puree mịn dùng pha chế.", "79000.00", "/images/puree_mango.jpg", Units.PIECE, "Chai 500ml", 100)
        ));

        // ===== GHÉP & SEED: tối đa 10 sản phẩm / danh mục =====
        java.util.concurrent.atomic.AtomicInteger brandIdx = new java.util.concurrent.atomic.AtomicInteger();
        int created = 0;

        for (var cate : savedCategories) {
            String cateName;
            try {
                cateName = (String) cate.getClass().getMethod("getName").invoke(cate);
            } catch (Exception e) { continue; }

            var rows = data.get(cateName);
            if (rows == null || rows.isEmpty()) {
                log.info("Không có dataset cho danh mục '{}', bỏ qua", cateName);
                continue;
            }

            Units defaultUnit = unitForCategory.apply(cateName);

            rows.stream().limit(10).forEach(r -> {
                Brands brand = savedBrands.get(brandIdx.getAndIncrement() % savedBrands.size());
                Units unit = (r.unit != null) ? r.unit : defaultUnit;
                String variantName = (r.variant != null && !r.variant.isBlank()) ? r.variant : variantNameOf.apply(unit);

                upsertProductWithVariant(
                        r.name,
                        r.desc,
                        cate,
                        brand,
                        unit,
                        variantName,
                        new java.math.BigDecimal(r.price),
                        r.stock,
                        r.image
                );
            });

            created += Math.min(10, rows.size());
        }

        log.info("Seed sản phẩm hoàn tất (chủ đề trái cây). Đã upsert ~{} sản phẩm (tối đa 10/ danh mục).", created);
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