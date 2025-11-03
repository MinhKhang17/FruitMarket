package com.example.fruitmarket.config;

import com.example.fruitmarket.enums.ProductStatus;
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

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedCategories();
        seedBrands();
        seedDefaultUser();
        seedProducts();
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
            userDetail.setAddress("Ho Chi Minh");
            userDetail.setPhone("0933567467");
            userDetail.setUser(savedUser);
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

            // set product image (served from /images/red_apple.jpg)
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

            ProductVariant v12 = new ProductVariant();
            v12.setVariant_name("500g");
            v12.setPrice(new BigDecimal("25000.00"));
            v12.setProduct(p1);
            v12.setStock(100);
            v12.setImage(new Image("/images/baby_spinach"));
            v12.setStatus(ProductStatus.ACTIVE);
            try {
                v12.getClass().getMethod("setImageUrl", String.class).invoke(v12, "/images/red_apple.jpg");
            } catch (Exception ignored) {
            }

            p1.getVariants().add(v11);
            p1.getVariants().add(v12);

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

            setImageIfPossible.accept(p2, "/images/baby_spinach.jpg");

            ProductVariant s21 = new ProductVariant();
            s21.setVariant_name("250g");
            s21.setPrice(new BigDecimal("24000.00"));
            s21.setProduct(p2);
            s21.setStock(80);
            s21.setStatus(ProductStatus.ACTIVE);
            try {
                s21.getClass().getMethod("setImageUrl", String.class).invoke(s21, "/images/baby_spinach.jpg");
            } catch (Exception ignored) {
            }

            ProductVariant s22 = new ProductVariant();
            s22.setVariant_name("500g");
            s22.setPrice(new BigDecimal("42000.00"));
            s22.setProduct(p2);
            s22.setStock(60);
            s22.setStatus(ProductStatus.ACTIVE);
            try {
                s22.getClass().getMethod("setImageUrl", String.class).invoke(s22, "/images/baby_spinach.jpg");
            } catch (Exception ignored) {
            }

            p2.getVariants().add(s21);
            p2.getVariants().add(s22);

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

            setImageIfPossible.accept(p3, "/images/dried_mango.jpg");

            ProductVariant m31 = new ProductVariant();
            m31.setVariant_name("200g");
            m31.setPrice(new BigDecimal("55000.00"));
            m31.setProduct(p3);
            m31.setStock(120);
            m31.setStatus(ProductStatus.ACTIVE);
            try {
                m31.getClass().getMethod("setImageUrl", String.class).invoke(m31, "/images/dried_mango.jpg");
            } catch (Exception ignored) {
            }

            ProductVariant m32 = new ProductVariant();
            m32.setVariant_name("500g");
            m32.setPrice(new BigDecimal("120000.00"));
            m32.setProduct(p3);
            m32.setStock(60);
            m32.setStatus(ProductStatus.ACTIVE);
            try {
                m32.getClass().getMethod("setImageUrl", String.class).invoke(m32, "/images/dried_mango.jpg");
            } catch (Exception ignored) {
            }

            p3.getVariants().add(m31);
            p3.getVariants().add(m32);

            productRepository.save(p3);
            log.info("Seeded product Dried Mango with variants");
        } else {
            log.info("Product 'Dried Mango' exists, skipping");
        }

        // NOTE: do not link placeholder.png except as fallback in UI/templates
        log.info("Product seeding complete");
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