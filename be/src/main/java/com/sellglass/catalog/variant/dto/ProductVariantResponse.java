package com.sellglass.catalog.variant.dto;

import com.sellglass.catalog.variant.ProductVariant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductVariantResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private String color;
    private String size;
    private BigDecimal price;
    private boolean isActive;
    private int stock;

    public static ProductVariantResponse from(ProductVariant variant) {
        ProductVariantResponse response = new ProductVariantResponse();
        response.id = variant.getId();
        response.productId = variant.getProductId();
        response.sku = variant.getSku();
        response.color = variant.getColor();
        response.size = variant.getSize();
        response.price = variant.getPrice();
        response.isActive = variant.isActive();
        response.stock = variant.getStock();
        return response;
    }
}
