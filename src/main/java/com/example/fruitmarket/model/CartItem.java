package com.example.fruitmarket.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CartItem {
    private Long productId;
    private Long variantId;
    private String name;
    private String variantName;
    private BigDecimal price = BigDecimal.ZERO;
    private Integer quantity = 1;
    private Double weight;
    private String imageUrl;
    private String unit;

    public CartItem(Long productId, Long variantId, String name, String variantName,
                    BigDecimal price, int quantity, Double weight, String imageUrl, String unit) {
        this.productId = productId;
        this.variantId = variantId;
        this.name = name;
        this.variantName = variantName;
        this.price = (price != null) ? price : BigDecimal.ZERO;
        this.quantity = (quantity > 0) ? quantity : 1;
        this.weight = (weight != null && weight > 0) ? weight : null; // ✅ chỉ set khi có giá trị thật
        this.imageUrl = imageUrl;
        this.unit = unit;
    }

    public BigDecimal getSubTotal() {
        if (price == null) return BigDecimal.ZERO;
        if ("KILOGRAM".equalsIgnoreCase(unit)) {
            return price.multiply(BigDecimal.valueOf(getWeight()));
        } else {
            return price.multiply(BigDecimal.valueOf(getQuantity()));
        }
    }

    public double getWeight() {
        return (weight != null && weight > 0) ? weight : 0.0;
    }
}
