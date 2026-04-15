package com.sellglass.catalog.brand;

import com.sellglass.catalog.brand.dto.BrandRequest;
import com.sellglass.catalog.brand.dto.BrandResponse;
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
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl service;

    private UUID brandId;
    private Brand brand;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
        brand = new Brand();
        brand.setId(brandId);
        brand.setName("Ray-Ban");
        brand.setSlug("ray-ban");
        brand.setLogoUrl("https://cdn.example.com/ray-ban.png");
        brand.setActive(true);
    }

    private BrandRequest buildRequest(String slug) {
        BrandRequest request = new BrandRequest();
        request.setName("Ray-Ban");
        request.setSlug(slug);
        request.setLogoUrl("https://cdn.example.com/ray-ban.png");
        request.setActive(true);
        return request;
    }

    @Test
    @DisplayName("findAll should return all brands")
    void findAll_success() {
        when(brandRepository.findAll()).thenReturn(List.of(brand));

        List<BrandResponse> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Ray-Ban");
    }

    @Test
    @DisplayName("findById should return brand when exists")
    void findById_success() {
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));

        BrandResponse result = service.findById(brandId);

        assertThat(result.getSlug()).isEqualTo("ray-ban");
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when brand missing")
    void findById_notFound() {
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(brandId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save brand when slug is unique")
    void create_success() {
        when(brandRepository.existsBySlug("ray-ban")).thenReturn(false);
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> {
            Brand b = inv.getArgument(0);
            b.setId(brandId);
            return b;
        });

        BrandResponse result = service.create(buildRequest("ray-ban"));

        assertThat(result.getName()).isEqualTo("Ray-Ban");
        assertThat(result.getSlug()).isEqualTo("ray-ban");
    }

    @Test
    @DisplayName("create should throw CONFLICT when slug already in use")
    void create_slugConflict() {
        when(brandRepository.existsBySlug("ray-ban")).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildRequest("ray-ban")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should modify brand fields")
    void update_success() {
        BrandRequest request = buildRequest("ray-ban"); // same slug, no conflict check
        request.setName("Ray-Ban Updated");

        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        BrandResponse result = service.update(brandId, request);

        assertThat(result.getName()).isEqualTo("Ray-Ban Updated");
    }

    @Test
    @DisplayName("update should throw CONFLICT when changing to existing slug")
    void update_slugConflict() {
        BrandRequest request = buildRequest("oakley"); // different slug that exists
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.existsBySlug("oakley")).thenReturn(true);

        assertThatThrownBy(() -> service.update(brandId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(brandRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should allow same slug without conflict")
    void update_sameSlug_noConflict() {
        BrandRequest request = buildRequest("ray-ban");
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(brandId, request);

        verify(brandRepository, never()).existsBySlug(any());
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when brand missing")
    void update_notFound() {
        when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(brandId, buildRequest("ray-ban")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("delete should remove brand when exists")
    void delete_success() {
        when(brandRepository.existsById(brandId)).thenReturn(true);

        service.delete(brandId);

        verify(brandRepository).deleteById(brandId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when brand missing")
    void delete_notFound() {
        when(brandRepository.existsById(brandId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(brandId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(brandRepository, never()).deleteById(any());
    }
}
