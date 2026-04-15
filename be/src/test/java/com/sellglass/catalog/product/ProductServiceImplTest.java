package com.sellglass.catalog.product;

import com.sellglass.catalog.brand.Brand;
import com.sellglass.catalog.brand.BrandRepository;
import com.sellglass.catalog.category.Category;
import com.sellglass.catalog.category.CategoryRepository;
import com.sellglass.catalog.product.dto.ProductRequest;
import com.sellglass.catalog.product.dto.ProductResponse;
import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
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
class ProductServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;

    @InjectMocks
    private ProductServiceImpl service;

    private UUID productId;
    private UUID categoryId;
    private UUID brandId;
    private Product product;
    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        brandId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setName("Kính mát");
        category.setSlug("kinh-mat");

        brand = new Brand();
        brand.setId(brandId);
        brand.setName("Ray-Ban");
        brand.setSlug("ray-ban");

        product = new Product();
        product.setId(productId);
        product.setName("Kính Ray-Ban Classic");
        product.setSlug("kinh-ray-ban-classic");
        product.setCategoryId(categoryId);
        product.setBrandId(brandId);
        product.setGender(Product.Gender.UNISEX);
        product.setActive(true);
    }

    private void mockBuildProductResponse() {
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of());
        when(productVariantRepository.findByProductIdAndIsActiveTrue(productId))
                .thenReturn(List.of());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
    }

    private ProductRequest buildRequest(String slug) {
        ProductRequest request = new ProductRequest();
        request.setName("Kính Ray-Ban Classic");
        request.setSlug(slug);
        request.setCategoryId(categoryId);
        request.setBrandId(brandId);
        request.setGender(Product.Gender.UNISEX);
        request.setActive(true);
        return request;
    }

    @Test
    @DisplayName("findBySlug should return product with images, variants, category and brand")
    void findBySlug_success() {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProductId(productId);
        variant.setSku("SKU-001");
        variant.setPrice(new BigDecimal("500000"));
        variant.setActive(true);

        ProductImage image = new ProductImage();
        image.setId(UUID.randomUUID());
        image.setProductId(productId);
        image.setUrl("https://cdn.example.com/img.jpg");
        image.setPrimary(true);

        when(productRepository.findBySlugAndIsActiveTrue("kinh-ray-ban-classic"))
                .thenReturn(Optional.of(product));
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(image));
        when(productVariantRepository.findByProductIdAndIsActiveTrue(productId))
                .thenReturn(List.of(variant));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));

        ProductResponse result = service.findBySlug("kinh-ray-ban-classic");

        assertThat(result.getName()).isEqualTo("Kính Ray-Ban Classic");
        assertThat(result.getCategoryName()).isEqualTo("Kính mát");
        assertThat(result.getBrandName()).isEqualTo("Ray-Ban");
        assertThat(result.getImages()).hasSize(1);
        assertThat(result.getVariants()).hasSize(1);
    }

    @Test
    @DisplayName("findBySlug should throw NOT_FOUND when product missing or inactive")
    void findBySlug_notFound() {
        when(productRepository.findBySlugAndIsActiveTrue("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBySlug("missing"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("findById should return product when exists")
    void findById_success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        mockBuildProductResponse();

        ProductResponse result = service.findById(productId);

        assertThat(result.getId()).isEqualTo(productId);
        assertThat(result.getSlug()).isEqualTo("kinh-ray-ban-classic");
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when product missing")
    void findById_notFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(productId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save product when slug is unique")
    void create_success() {
        when(productRepository.existsBySlug("kinh-ray-ban-classic")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });
        mockBuildProductResponse();

        ProductResponse result = service.create(buildRequest("kinh-ray-ban-classic"));

        assertThat(result.getName()).isEqualTo("Kính Ray-Ban Classic");
        assertThat(result.getCategoryName()).isEqualTo("Kính mát");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("create should throw CONFLICT when slug already in use")
    void create_slugConflict() {
        when(productRepository.existsBySlug("kinh-ray-ban-classic")).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildRequest("kinh-ray-ban-classic")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should modify product fields")
    void update_success() {
        ProductRequest request = buildRequest("kinh-ray-ban-classic"); // same slug
        request.setName("Updated Name");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        mockBuildProductResponse();

        ProductResponse result = service.update(productId, request);

        assertThat(result.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("update should throw CONFLICT when changing to existing slug")
    void update_slugConflict() {
        ProductRequest request = buildRequest("other-slug");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlug("other-slug")).thenReturn(true);

        assertThatThrownBy(() -> service.update(productId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should allow same slug without conflict check")
    void update_sameSlug_noConflict() {
        ProductRequest request = buildRequest("kinh-ray-ban-classic");
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        mockBuildProductResponse();

        service.update(productId, request);

        verify(productRepository, never()).existsBySlug(any());
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when product missing")
    void update_notFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(productId, buildRequest("any-slug")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("delete should remove product when exists")
    void delete_success() {
        when(productRepository.existsById(productId)).thenReturn(true);

        service.delete(productId);

        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when product missing")
    void delete_notFound() {
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(productId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(productRepository, never()).deleteById(any());
    }
}
