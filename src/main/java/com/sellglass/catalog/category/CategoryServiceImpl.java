package com.sellglass.catalog.category;

import com.sellglass.catalog.category.dto.CategoryRequest;
import com.sellglass.catalog.category.dto.CategoryResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Override
    public CategoryResponse findById(UUID id) {
        return CategoryResponse.from(categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Category not found")));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setActive(request.isActive());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Category not found"));
        if (!category.getSlug().equals(request.getSlug()) && categoryRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setActive(request.isActive());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteById(id);
    }
}
