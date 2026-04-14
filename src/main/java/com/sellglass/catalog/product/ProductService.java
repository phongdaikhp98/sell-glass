package com.sellglass.catalog.product;

import com.sellglass.catalog.product.dto.ProductListResponse;
import com.sellglass.catalog.product.dto.ProductRequest;
import com.sellglass.catalog.product.dto.ProductResponse;
import com.sellglass.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    PageResponse<ProductListResponse> findAll(Pageable pageable);

    ProductResponse findById(UUID id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);
}
