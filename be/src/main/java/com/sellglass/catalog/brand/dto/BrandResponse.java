package com.sellglass.catalog.brand.dto;

import com.sellglass.catalog.brand.Brand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BrandResponse {

    private UUID id;
    private String name;
    private String slug;
    private String logoUrl;
    private boolean isActive;

    public static BrandResponse from(Brand brand) {
        BrandResponse response = new BrandResponse();
        response.id = brand.getId();
        response.name = brand.getName();
        response.slug = brand.getSlug();
        response.logoUrl = brand.getLogoUrl();
        response.isActive = brand.isActive();
        return response;
    }
}
