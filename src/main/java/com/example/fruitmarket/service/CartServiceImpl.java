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
import java.util.Map;
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

    /**
     * ✅ Thêm sản phẩm vào giỏ (tách rõ logic quantity / weight)
     */
    @Override
    public void addToCart(Long productId, Long variantId, double qtyOrWeight) {
        Cart cart = getOrCreateCart();

        String name = "Unknown";
        BigDecimal price = BigDecimal.ZERO;
        String img = "/images/placeholder.png";
        String unit = "PIECE";
        String variantName = null;

        if (variantId != null) {
            Optional<ProductVariant> optVar = productVariantRepository.findById(variantId);
            if (optVar.isPresent()) {
                ProductVariant v = optVar.get();
                price = (v.getPrice() != null) ? v.getPrice() : BigDecimal.ZERO;
                name = v.getProduct().getProductName();
                variantName = v.getVariant_name();
                unit = v.getProduct().getUnit().toString();
                if (v.getImage() != null && v.getImage().getUrl() != null) {
                    img = v.getImage().getUrl();
                }
            }
        } else {
            Optional<Product> optProd = productRepository.findById(productId);
            if (optProd.isPresent()) {
                Product prod = optProd.get();
                name = prod.getProductName();
                unit = prod.getUnit().toString();
                if (prod.getVariants() != null && !prod.getVariants().isEmpty()) {
                    price = prod.getVariants().get(0).getPrice() != null
                            ? prod.getVariants().get(0).getPrice()
                            : BigDecimal.ZERO;
                }
            }
        }

        CartItem item;
        if ("KILOGRAM".equalsIgnoreCase(unit)) {
            double w = (qtyOrWeight > 0) ? qtyOrWeight : 0.1;
            item = new CartItem(productId, variantId, name, variantName, price, 0, w, img, unit);
        } else {
            int q = (int) Math.max(1, Math.floor(qtyOrWeight));
            item = new CartItem(productId, variantId, name, variantName, price, q, null, img, unit);
        }


        cart.addItem(item);
        session.setAttribute(SESSION_CART, cart);
    }
    @Override
    public Cart getCart() {
        return getOrCreateCart();
    }

    /** ✅ Cho phép update cả số lượng lẫn khối lượng */
    @Override
    public void updateQuantity(Long productId, Long variantId, double qtyOrWeight) {
        Cart cart = getOrCreateCart();
        cart.updateQuantity(productId, variantId, qtyOrWeight);
        session.setAttribute(SESSION_CART, cart);
    }
    @Override
    public void remove(Long productId, Long variantId) {
        Cart cart = getOrCreateCart();
        cart.removeItem(productId, variantId);
        session.setAttribute(SESSION_CART, cart);
    }
    @Override
    public void clear() {
        Cart cart = getOrCreateCart();
        cart.clear();
        session.setAttribute(SESSION_CART, cart);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, CartItem> getCart(HttpSession session) {
        Object cart = session.getAttribute("cart");
        if (cart == null) {
            return java.util.Collections.emptyMap();
        }
        return (Map<Long, CartItem>) cart;
    }

    @Override
    public int getTotalQuantity(HttpSession session) {
        return getCart(session).values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    public BigDecimal getSubtotal(HttpSession session) {
        return getCart(session).values().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
