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
public class ProductResponse {

    private UUID id;
    private UUID categoryId;
    private UUID brandId;
    private String name;
    private String slug;
    private String description;
    private String frameShape;
    private String material;
    private Product.Gender gender;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product product) {
        ProductResponse response = new ProductResponse();
        response.id = product.getId();
        response.categoryId = product.getCategoryId();
        response.brandId = product.getBrandId();
        response.name = product.getName();
        response.slug = product.getSlug();
        response.description = product.getDescription();
        response.frameShape = product.getFrameShape();
        response.material = product.getMaterial();
        response.gender = product.getGender();
        response.isActive = product.isActive();
        response.createdAt = product.getCreatedAt();
        response.updatedAt = product.getUpdatedAt();
        return response;
    }
}
