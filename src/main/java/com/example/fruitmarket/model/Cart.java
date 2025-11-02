package com.example.fruitmarket.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Giỏ hàng (Cart) lưu trong session — an toàn với trường hợp variantId = null.
 */
public class Cart {

    private final Map<String, CartItem> items = new LinkedHashMap<>();

    /**
     * Thêm sản phẩm vào giỏ.
     * Nếu key (product + variant) đã tồn tại thì cộng dồn số lượng.
     */
    public void addItem(CartItem item) {
        if (item == null) return;

        String key = buildKey(item.getVariantId(), item.getVariantId());

        if (items.containsKey(key)) {
            CartItem existing = items.get(key);
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
        } else {
            items.put(key, item);
        }
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ.
     */
    public void updateQuantity(Long productId, Long variantId, int qty) {
        String key = buildKey(productId, variantId);
        CartItem it = items.get(key);
        if (it != null) {
            if (qty <= 0) items.remove(key);
            else it.setQuantity(qty);
        }
    }

    /**
     * Xoá sản phẩm khỏi giỏ.
     */
    public void removeItem(Long productId, Long variantId) {
        items.remove(buildKey(productId, variantId));
    }

    /**
     * Lấy danh sách item trong giỏ.
     */
    public Collection<CartItem> getItems() {
        return items.values();
    }

    /**
     * Tổng số lượng sản phẩm trong giỏ.
     */
    public int getTotalQuantity() {
        return items.values().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Tổng tiền trong giỏ hàng.
     */
    public BigDecimal getTotalPrice() {
        return items.values().stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Xoá toàn bộ giỏ hàng.
     */
    public void clear() {
        items.clear();
    }

    /**
     * Tạo key duy nhất dựa trên productId và variantId.
     * Xử lý an toàn khi variantId = null.
     */
    private String buildKey(Long productId, Long variantId) {
        String p = (productId != null) ? productId.toString() : "pnull";
        String v = (variantId != null) ? variantId.toString() : "vnull";
        return p + "#" + v;
    }

    public boolean isEmpty() {
        if (items.isEmpty()) return true;
        return false;
    }
}
