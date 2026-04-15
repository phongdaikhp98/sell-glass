package com.sellglass.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private UUID cartId;
    private List<CartItemDetail> items;
    private BigDecimal total;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDetail {
        private UUID itemId;
        private UUID productVariantId;
        private String productName;
        private String primaryImageUrl;
        private String sku;
        private String color;
        private String size;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }
}
