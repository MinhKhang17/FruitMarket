package com.example.fruitmarket.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CartItem {
    // product id (bắt buộc để form gửi productId tới controller)
    private Long productId;

    // variant id nếu có (nullable)
    private Long variantId;

    // hiển thị tên sản phẩm
    private String name;         // tương đương productName

    // tên biến thể (nếu muốn hiển thị)
    private String variantName;

    // đơn giá (unit price)
    private BigDecimal price = BigDecimal.ZERO;

    // số lượng
    private Integer quantity = 1;

    // url ảnh hiển thị
    private String imageUrl;

    public CartItem(Long productId, Long variantId, String name, String variantName,
                    BigDecimal price, int quantity, String imageUrl) {
        this.productId = productId;
        this.variantId = variantId;
        this.name = name;
        this.variantName = variantName;
        this.price = (price != null) ? price : BigDecimal.ZERO;
        this.quantity = Math.max(1, quantity);
        this.imageUrl = imageUrl;
    }

    // backward-compatible constructors (giữ tương thích với code cũ)
    public CartItem(Long variantId, String name, BigDecimal price, int quantity, String imageUrl) {
        this(null, variantId, name, null, price, quantity, imageUrl);
    }

    // tính subtotal an toàn
    public BigDecimal getSubTotal() {
        if (price == null) return BigDecimal.ZERO;
        int q = (quantity == null || quantity < 1) ? 1 : quantity;
        return price.multiply(BigDecimal.valueOf(q));
    }
}
