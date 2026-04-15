package com.sellglass.catalog.variant;

import com.sellglass.catalog.variant.dto.ProductVariantRequest;
import com.sellglass.catalog.variant.dto.ProductVariantResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceImplTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @InjectMocks
    private ProductVariantServiceImpl service;

    private UUID productId;
    private UUID variantId;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku("SKU-001");
        variant.setColor("Black");
        variant.setSize("M");
        variant.setPrice(new BigDecimal("500000"));
        variant.setActive(true);
    }

    private ProductVariantRequest buildRequest(String sku) {
        ProductVariantRequest request = new ProductVariantRequest();
        request.setSku(sku);
        request.setColor("Black");
        request.setSize("M");
        request.setPrice(new BigDecimal("500000"));
        request.setActive(true);
        return request;
    }

    @Test
    @DisplayName("findByProductId should return all variants for product")
    void findByProductId_success() {
        when(variantRepository.findByProductId(productId)).thenReturn(List.of(variant));

        List<ProductVariantResponse> result = service.findByProductId(productId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSku()).isEqualTo("SKU-001");
    }

    @Test
    @DisplayName("findById should return variant when exists")
    void findById_success() {
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        ProductVariantResponse result = service.findById(variantId);

        assertThat(result.getId()).isEqualTo(variantId);
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when variant missing")
    void findById_notFound() {
        when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(variantId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save variant")
    void create_success() {
        when(variantRepository.existsBySku("SKU-NEW")).thenReturn(false);
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
            ProductVariant v = inv.getArgument(0);
            v.setId(variantId);
            return v;
        });

        ProductVariantResponse result = service.create(productId, buildRequest("SKU-NEW"));

        assertThat(result.getSku()).isEqualTo("SKU-NEW");
        assertThat(result.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("create should throw CONFLICT when SKU already in use")
    void create_skuConflict() {
        when(variantRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(productId, buildRequest("SKU-001")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(variantRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should modify variant fields")
    void update_success() {
        ProductVariantRequest request = buildRequest("SKU-001"); // same SKU, no conflict
        request.setColor("White");
        request.setPrice(new BigDecimal("600000"));

        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariantResponse result = service.update(variantId, request);

        assertThat(result.getColor()).isEqualTo("White");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("600000"));
    }

    @Test
    @DisplayName("update should throw CONFLICT when changing to an existing SKU")
    void update_skuConflict() {
        ProductVariantRequest request = buildRequest("SKU-OTHER");
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsBySku("SKU-OTHER")).thenReturn(true);

        assertThatThrownBy(() -> service.update(variantId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(variantRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should allow same SKU on same variant without conflict")
    void update_sameSku_noConflict() {
        ProductVariantRequest request = buildRequest("SKU-001"); // same as existing
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductVariantResponse result = service.update(variantId, request);

        assertThat(result.getSku()).isEqualTo("SKU-001");
        verify(variantRepository, never()).existsBySku(any()); // no check needed when SKU unchanged
    }

    @Test
    @DisplayName("delete should remove variant when exists")
    void delete_success() {
        when(variantRepository.existsById(variantId)).thenReturn(true);

        service.delete(variantId);

        verify(variantRepository).deleteById(variantId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when variant missing")
    void delete_notFound() {
        when(variantRepository.existsById(variantId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(variantId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(variantRepository, never()).deleteById(any());
    }
}
