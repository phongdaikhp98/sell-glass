package com.sellglass.catalog.product.dto;

import com.sellglass.catalog.product.Product;
import com.sellglass.catalog.product.ProductImage;
import com.sellglass.catalog.variant.ProductVariant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String name;
    private String slug;
    private String description;
    private String frameShape;
    private String material;
    private Product.Gender gender;
    private boolean isActive;
    private List<ImageDto> images;
    private List<VariantDto> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ImageDto {
        private UUID id;
        private String url;
        private int sortOrder;
        private boolean isPrimary;

        public static ImageDto from(ProductImage img) {
            ImageDto dto = new ImageDto();
            dto.id = img.getId();
            dto.url = img.getUrl();
            dto.sortOrder = img.getSortOrder();
            dto.isPrimary = img.isPrimary();
            return dto;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VariantDto {
        private UUID id;
        private String sku;
        private String color;
        private String size;
        private BigDecimal price;
        private boolean isActive;

        public static VariantDto from(ProductVariant v) {
            VariantDto dto = new VariantDto();
            dto.id = v.getId();
            dto.sku = v.getSku();
            dto.color = v.getColor();
            dto.size = v.getSize();
            dto.price = v.getPrice();
            dto.isActive = v.isActive();
            return dto;
        }
    }

    public static ProductResponse from(Product product, List<ProductImage> images, List<ProductVariant> variants,
                                       String categoryName, String brandName) {
        ProductResponse response = new ProductResponse();
        response.id = product.getId();
        response.categoryId = product.getCategoryId();
        response.categoryName = categoryName;
        response.brandId = product.getBrandId();
        response.brandName = brandName;
        response.name = product.getName();
        response.slug = product.getSlug();
        response.description = product.getDescription();
        response.frameShape = product.getFrameShape();
        response.material = product.getMaterial();
        response.gender = product.getGender();
        response.isActive = product.isActive();
        response.images = images.stream().map(ImageDto::from).toList();
        response.variants = variants.stream().map(VariantDto::from).toList();
        response.createdAt = product.getCreatedAt();
        response.updatedAt = product.getUpdatedAt();
        return response;
    }
}
