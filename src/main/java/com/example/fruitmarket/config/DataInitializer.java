// java
package com.example.fruitmarket.config;

import com.example.fruitmarket.model.Brands;
import com.example.fruitmarket.model.Categorys;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.repository.BrandsRepository;
import com.example.fruitmarket.repository.CategorysRepository;
import com.example.fruitmarket.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
            List<Categorys> savedCategories = categorysRepository.findAll();
            List<Brands> savedBrands = brandsRepository.findAll();

            // defensive: ensure we have at least one category and brand
            if (!savedCategories.isEmpty() && !savedBrands.isEmpty()) {
                Categorys c1 = savedCategories.get(0);
                Categorys c2 = savedCategories.size() > 1 ? savedCategories.get(1) : c1;
                Brands b1 = savedBrands.get(0);
                Brands b2 = savedBrands.size() > 1 ? savedBrands.get(1) : b1;

                Product p1 = new Product();
                p1.setProduct_name("Red Apple");
                p1.setProduct_description("Crisp, sweet red apples - 1kg pack");
                p1.setProduct_price(2.99);
                p1.setImages(new ArrayList<>()); // no images for seeding
                p1.setCategory(c1);
                p1.setBrand(b1);

                Product p2 = new Product();
                p2.setProduct_name("Baby Spinach");
                p2.setProduct_description("Fresh baby spinach - 200g");
                p2.setProduct_price(1.49);
                p2.setImages(new ArrayList<>());
                p2.setCategory(c2);
                p2.setBrand(b2);

                Product p3 = new Product();
                p3.setProduct_name("Dried Mango");
                p3.setProduct_description("Sweet dried mango slices - 250g");
                p3.setProduct_price(4.50);
                p3.setImages(new ArrayList<>());
                p3.setCategory(savedCategories.get(savedCategories.size() - 1));
                p3.setBrand(b1);

                List<Product> products = List.of(p1, p2, p3);
                productRepository.saveAll(products);
                log.info("Seeded {} products", products.size());
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
