package com.sellglass.catalog.product;

import com.sellglass.catalog.brand.Brand;
import com.sellglass.catalog.brand.BrandRepository;
import com.sellglass.catalog.category.Category;
import com.sellglass.catalog.category.CategoryRepository;
import com.sellglass.catalog.product.dto.ProductListResponse;
import com.sellglass.catalog.product.dto.ProductRequest;
import com.sellglass.catalog.product.dto.ProductResponse;
import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    public PageResponse<ProductListResponse> findAll(String search, UUID categoryId, UUID brandId,
                                                     Product.Gender gender, Pageable pageable) {
        Page<Product> productPage = productRepository.findWithFilters(search, categoryId, brandId, gender, pageable);
        List<Product> products = productPage.getContent();

        if (products.isEmpty()) {
            return PageResponse.of(productPage.map(p -> null));
        }

        Set<UUID> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());

        // Batch load primary images
        Map<UUID, String> primaryImageMap = productImageRepository.findByProductIdIn(productIds).stream()
                .filter(ProductImage::isPrimary)
                .collect(Collectors.toMap(ProductImage::getProductId, ProductImage::getUrl,
                        (existing, replacement) -> existing));

        // Batch load min price from variants
        Map<UUID, BigDecimal> minPriceMap = productVariantRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(
                        ProductVariant::getProductId,
                        Collectors.mapping(ProductVariant::getPrice,
                                Collectors.minBy(BigDecimal::compareTo))
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));

        // Batch load category names
        Set<UUID> categoryIds = products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        Map<UUID, String> categoryNameMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // Batch load brand names
        Set<UUID> brandIds = products.stream().map(Product::getBrandId).collect(Collectors.toSet());
        Map<UUID, String> brandNameMap = brandRepository.findAllById(brandIds).stream()
                .collect(Collectors.toMap(Brand::getId, Brand::getName));

        return PageResponse.of(productPage.map(product -> ProductListResponse.from(
                product,
                primaryImageMap.get(product.getId()),
                minPriceMap.get(product.getId()),
                categoryNameMap.get(product.getCategoryId()),
                brandNameMap.get(product.getBrandId())
        )));
    }

    @Override
    public ProductResponse findBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Product not found"));
        return buildProductResponse(product);
    }

    @Override
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Product not found"));
        return buildProductResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        Product product = new Product();
        mapRequestToEntity(request, product);
        return buildProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Product not found"));
        if (!product.getSlug().equals(request.getSlug()) && productRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        mapRequestToEntity(request, product);
        return buildProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
    }

    private ProductResponse buildProductResponse(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId());

        String categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName)
                .orElse(null);
        String brandName = brandRepository.findById(product.getBrandId())
                .map(Brand::getName)
                .orElse(null);

        return ProductResponse.from(product, images, variants, categoryName, brandName);
    }

    private void mapRequestToEntity(ProductRequest request, Product product) {
        product.setCategoryId(request.getCategoryId());
        product.setBrandId(request.getBrandId());
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setFrameShape(request.getFrameShape());
        product.setMaterial(request.getMaterial());
        product.setGender(request.getGender());
        product.setActive(request.isActive());
    }
}
