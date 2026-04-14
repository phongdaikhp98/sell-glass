package com.sellglass.catalog.variant;

import com.sellglass.catalog.variant.dto.ProductVariantRequest;
import com.sellglass.catalog.variant.dto.ProductVariantResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {

    List<ProductVariantResponse> findByProductId(UUID productId);

    ProductVariantResponse findById(UUID id);

    ProductVariantResponse create(UUID productId, ProductVariantRequest request);

    ProductVariantResponse update(UUID id, ProductVariantRequest request);

    void delete(UUID id);
}
