package com.sellglass.catalog.category;

import com.sellglass.catalog.category.dto.CategoryRequest;
import com.sellglass.catalog.category.dto.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> findAll();

    CategoryResponse findById(UUID id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    void delete(UUID id);
}
