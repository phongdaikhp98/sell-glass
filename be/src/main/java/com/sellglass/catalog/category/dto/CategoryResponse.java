package com.sellglass.catalog.category.dto;

import com.sellglass.catalog.category.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private boolean isActive;

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        response.slug = category.getSlug();
        response.isActive = category.isActive();
        return response;
    }
}
