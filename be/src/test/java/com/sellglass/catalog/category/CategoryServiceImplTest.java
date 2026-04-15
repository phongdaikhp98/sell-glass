package com.sellglass.catalog.category;

import com.sellglass.catalog.category.dto.CategoryRequest;
import com.sellglass.catalog.category.dto.CategoryResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl service;

    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = new Category();
        category.setId(categoryId);
        category.setName("Kính mát");
        category.setSlug("kinh-mat");
        category.setActive(true);
    }

    private CategoryRequest buildRequest(String slug) {
        CategoryRequest request = new CategoryRequest();
        request.setName("Kính mát");
        request.setSlug(slug);
        request.setActive(true);
        return request;
    }

    @Test
    @DisplayName("findAll should return all categories")
    void findAll_success() {
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<CategoryResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSlug()).isEqualTo("kinh-mat");
    }

    @Test
    @DisplayName("findById should return category when exists")
    void findById_success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CategoryResponse result = service.findById(categoryId);

        assertThat(result.getName()).isEqualTo("Kính mát");
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when category missing")
    void findById_notFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(categoryId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save category when slug is unique")
    void create_success() {
        when(categoryRepository.existsBySlug("kinh-mat")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(categoryId);
            return c;
        });

        CategoryResponse result = service.create(buildRequest("kinh-mat"));

        assertThat(result.getName()).isEqualTo("Kính mát");
    }

    @Test
    @DisplayName("create should throw CONFLICT when slug already in use")
    void create_slugConflict() {
        when(categoryRepository.existsBySlug("kinh-mat")).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildRequest("kinh-mat")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should modify category")
    void update_success() {
        CategoryRequest request = buildRequest("kinh-mat");
        request.setName("Kính mát Updated");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse result = service.update(categoryId, request);

        assertThat(result.getName()).isEqualTo("Kính mát Updated");
    }

    @Test
    @DisplayName("update should throw CONFLICT when changing to existing slug")
    void update_slugConflict() {
        CategoryRequest request = buildRequest("kinh-can");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlug("kinh-can")).thenReturn(true);

        assertThatThrownBy(() -> service.update(categoryId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should allow same slug without conflict check")
    void update_sameSlug_noConflict() {
        CategoryRequest request = buildRequest("kinh-mat");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(categoryId, request);

        verify(categoryRepository, never()).existsBySlug(any());
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when category missing")
    void update_notFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(categoryId, buildRequest("kinh-mat")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("delete should remove category when exists")
    void delete_success() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        service.delete(categoryId);

        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when category missing")
    void delete_notFound() {
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(categoryId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(categoryRepository, never()).deleteById(any());
    }
}
