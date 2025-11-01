package com.example.fruitmarket.config;

import com.example.fruitmarket.model.*;
import com.example.fruitmarket.repository.*;
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
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserDetailRepo userDetailRepo;

    @Override
    public void run(String... args) throws Exception {

        if (categorysRepository.count() == 0) {
            List<Categorys> categories = List.of(
                    createCategory("Fruits", true),
                    createCategory("Vegetables", true),
                    createCategory("Dried Goods", true)
            );
            categorysRepository.saveAll(categories);
            log.info("Seeded {} categories", categories.size());
        }

        if (brandsRepository.count() == 0) {
            List<Brands> brands = List.of(
                    createBrand("FreshFarm", true),
                    createBrand("GreenGrow", true),
                    createBrand("NaturePack", true)
            );
            brandsRepository.saveAll(brands);
            log.info("Seeded {} brands", brands.size());
        }

        if (productRepository.count() == 0) {
            Users users = new Users();
            users.setUsername("user");
            users.setPassword(passwordEncoder.encode("password"));
            users.setRole("CLIENT");
            users.setPhone("0933456172");
            users.setEmail("user@gmail.com");
            users.setStatus("ACTIVE");
            User_detail userDetail = new User_detail();
            userDetail.setUser(userRepository.save(users));
            userDetail.setAddress("Ho Chi Minh");
            userDetail.setPhone("0933456789");
        userDetailRepo.save(userDetail);
            var savedCategories = categorysRepository.findAll();
            var savedBrands = brandsRepository.findAll();

            if (!savedCategories.isEmpty() && !savedBrands.isEmpty()) {
                Categorys c1 = savedCategories.get(0);
                Categorys c2 = savedCategories.size() > 1 ? savedCategories.get(1) : c1;
                Brands b1 = savedBrands.get(0);
                Brands b2 = savedBrands.size() > 1 ? savedBrands.get(1) : b1;

                Product p1 = new Product();
                p1.setProduct_name("Red Apple");
                p1.setProduct_description("Crisp, sweet red apples - base product");
                p1.setCategory(c1);
                p1.setBrand(b1);

                Product p2 = new Product();
                p2.setProduct_name("Baby Spinach");
                p2.setProduct_description("Fresh baby spinach - base product");
                p2.setCategory(c2);
                p2.setBrand(b2);

                Product p3 = new Product();
                p3.setProduct_name("Dried Mango");
                p3.setProduct_description("Sweet dried mango slices - base product");
                p3.setCategory(savedCategories.get(savedCategories.size() - 1));
                p3.setBrand(b1);

                // Tạo variants cho p1 (ví dụ 1kg, 500g)
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

                // Variants cho p2
                ProductVariant v21 = new ProductVariant();
                v21.setVariant_name("200g");
                v21.setPrice(new BigDecimal("29000.00"));
                v21.setProduct(p2);
                p2.getVariants().add(v21);

                // Variants cho p3
                ProductVariant v31 = new ProductVariant();
                v31.setVariant_name("250g");
                v31.setPrice(new BigDecimal("79000.00"));
                v31.setProduct(p3);
                v31.setStock(100);
                p3.getVariants().add(v31);

                // Save products (cascade sẽ lưu variants vì cascade = ALL)
                productRepository.saveAll(List.of(p1, p2, p3));
                log.info("Seeded products with variants");
            } else {
                log.warn("Skipping product seeding: categories or brands not present");
            }
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
