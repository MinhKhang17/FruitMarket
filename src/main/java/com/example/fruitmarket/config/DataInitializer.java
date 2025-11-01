package com.example.fruitmarket.config;

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
            u.setStatus("ACTIVE");
            Users savedUser = userRepository.save(u);

            User_detail userDetail = new User_detail();
            userDetail.setAddress("Ho Chi Minh");
            userDetail.setPhone("0933567467");
            userDetail.setUser(savedUser);
            userDetailRepo.save(userDetail);

            log.info("Seeded default user and userDetail");
        } else {
            log.info("Default user already exists, skipping user seed");
        }
    }

    private void seedProducts() {
        // Example for "Red Apple"
        if (!productRepository.existsByProductName("Red Apple")) {
            var savedCategories = categorysRepository.findAll();
            var savedBrands = brandsRepository.findAll();
            if (!savedCategories.isEmpty() && !savedBrands.isEmpty()) {
                Categorys c1 = savedCategories.get(0);
                Brands b1 = savedBrands.get(0);

                Product p1 = new Product();
                p1.setProductName("Red Apple");
                p1.setProduct_description("Crisp, sweet red apples - base product");
                p1.setCategory(c1);
                p1.setBrand(b1);

                ProductVariant v11 = new ProductVariant();
                v11.setVariant_name("1kg");
                v11.setPrice(new BigDecimal("49000.00"));
                v11.setProduct(p1);
                v11.setStock(100);

                ProductVariant v12 = new ProductVariant();
                v12.setVariant_name("500g");
                v12.setPrice(new BigDecimal("25000.00"));
                v12.setProduct(p1);
                v12.setStock(100);

                p1.getVariants().add(v11);
                p1.getVariants().add(v12);

                productRepository.save(p1);
                log.info("Seeded product Red Apple with variants");
            }
        } else {
            log.info("Product 'Red Apple' exists, skipping");
        }

        // Lặp lại tương tự cho các product khác (Baby Spinach, Dried Mango)...
        // IMPORTANT: always check existsByProductName(...) trước khi tạo mới
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