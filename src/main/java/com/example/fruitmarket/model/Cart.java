package com.example.fruitmarket.model;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {

    private final Map<String, CartItem> items = new LinkedHashMap<>();

    /** Thêm sản phẩm vào giỏ. Nếu trùng product+variant thì cộng dồn. */
    public void addItem(CartItem item) {
        if (item == null) return;
        String key = buildKey(item.getProductId(), item.getVariantId());

        if (items.containsKey(key)) {
            CartItem existing = items.get(key);
            if ("KILOGRAM".equalsIgnoreCase(existing.getUnit())) {
                double w = existing.getWeight() + (item.getWeight());
                existing.setWeight(w);
            } else {
                int q = existing.getQuantity() + (item.getQuantity() != null ? item.getQuantity() : 1);
                existing.setQuantity(q);
            }
        } else {
            items.put(key, item);
        }
    }


    /** Cập nhật số lượng hoặc khối lượng sản phẩm trong giỏ. */
    public void updateQuantity(Long productId, Long variantId, double qtyOrWeight) {
        String key = buildKey(productId, variantId);
        CartItem it = items.get(key);
        if (it == null) return;

        if ("KILOGRAM".equalsIgnoreCase(it.getUnit())) {
            double newWeight = Math.max(0.1, qtyOrWeight);
            it.setWeight(newWeight);
        } else {
            int newQty = (int) Math.max(1, Math.floor(qtyOrWeight));
            it.setQuantity(newQty);
        }
    }

    /** Xoá sản phẩm khỏi giỏ. */
    public void removeItem(Long productId, Long variantId) {
        items.remove(buildKey(productId, variantId));
    }

    /** Lấy toàn bộ item trong giỏ. */
    public Collection<CartItem> getItems() {
        return items.values();
    }

    /** Tổng tiền trong giỏ. */
    public BigDecimal getTotalPrice() {
        return items.values().stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Tổng số “mục” trong giỏ (cho badge hiển thị). */
    public int getTotalQuantity() {
        return items.size(); // ✅ mỗi item là 1 dòng trong giỏ
    }

    /** Xoá toàn bộ giỏ hàng. */
    public void clear() {
        items.clear();
    }

    private String buildKey(Long productId, Long variantId) {
        String p = (productId != null) ? productId.toString() : "pnull";
        String v = (variantId != null) ? variantId.toString() : "vnull";
        return p + "#" + v;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
