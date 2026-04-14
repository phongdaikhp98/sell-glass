package com.sellglass.catalog.product.dto;

import com.sellglass.catalog.product.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductListResponse {

    private UUID id;
    private String name;
    private String slug;
    private UUID categoryId;
    private UUID brandId;
    private String frameShape;
    private Product.Gender gender;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static ProductListResponse from(Product product) {
        ProductListResponse response = new ProductListResponse();
        response.id = product.getId();
        response.name = product.getName();
        response.slug = product.getSlug();
        response.categoryId = product.getCategoryId();
        response.brandId = product.getBrandId();
        response.frameShape = product.getFrameShape();
        response.gender = product.getGender();
        response.isActive = product.isActive();
        response.createdAt = product.getCreatedAt();
        return response;
    }
}
