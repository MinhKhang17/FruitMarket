package com.example.fruitmarket.service;

import com.example.fruitmarket.model.Cart;
import com.example.fruitmarket.model.CartItem;
import com.example.fruitmarket.model.Product;
import com.example.fruitmarket.model.ProductVariant;
import com.example.fruitmarket.repository.ProductRepository;
import com.example.fruitmarket.repository.ProductVariantRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    public static final String SESSION_CART = "CART_SESSION";

    private final HttpSession session;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private Cart getOrCreateCart() {
        Cart cart = (Cart) session.getAttribute(SESSION_CART);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(SESSION_CART, cart);
        }
        return cart;
    }

    public void addToCart(Long productId, Long variantId, int qty) {
        Cart cart = getOrCreateCart();
        // get product/variant info to fill CartItem
        String name = "Unknown";
        BigDecimal price = BigDecimal.ZERO;
        String img = "/images/placeholder.png";

        if (variantId != null) {
            Optional<ProductVariant> v = productVariantRepository.findById(variantId);
            if (v.isPresent()) {
                ProductVariant pv = v.get();
                price = pv.getPrice();
                name = pv.getVariant_name(); // or combine product name + variant
                if (pv.getImage() != null) img = pv.getImage().getUrl();
            }
        } else {
            Optional<Product> p = productRepository.findById(productId);
            if (p.isPresent()) {
                Product prod = p.get();
                name = prod.getProductName();
                if (prod.getImages() != null && !prod.getImages().isEmpty()) img = prod.getImages().get(0).getUrl();
                if (prod.getVariants() != null && !prod.getVariants().isEmpty()) {
                    price = prod.getVariants().get(0).getPrice();
                }
            }
        }

        CartItem item = new CartItem(variantId, name, price, qty, img);
        cart.addItem(item);
        session.setAttribute(SESSION_CART, cart);
    }

    public Cart getCart() {
        return getOrCreateCart();
    }

    public void updateQuantity(Long productId, Long variantId, int qty) {
        Cart cart = getOrCreateCart();
        cart.updateQuantity(productId, variantId, qty);
        session.setAttribute(SESSION_CART, cart);
    }

    public void remove(Long productId, Long variantId) {
        Cart cart = getOrCreateCart();
        cart.removeItem(productId, variantId);
        session.setAttribute(SESSION_CART, cart);
    }

    public void clear() {
        Cart cart = getOrCreateCart();
        cart.clear();
        session.setAttribute(SESSION_CART, cart);
    }
}
