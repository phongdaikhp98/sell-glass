package com.sellglass.catalog.product.dto;

import com.sellglass.catalog.product.ProductImage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductImageResponse {

    private UUID id;
    private UUID productId;
    private String url;
    private int sortOrder;
    private boolean isPrimary;

    public static ProductImageResponse from(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();
        response.id = image.getId();
        response.productId = image.getProductId();
        response.url = image.getUrl();
        response.sortOrder = image.getSortOrder();
        response.isPrimary = image.isPrimary();
        return response;
    }
}
