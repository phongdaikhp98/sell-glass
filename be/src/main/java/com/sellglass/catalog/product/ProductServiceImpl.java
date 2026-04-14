package com.sellglass.catalog.product;

import com.sellglass.catalog.product.dto.ProductListResponse;
import com.sellglass.catalog.product.dto.ProductRequest;
import com.sellglass.catalog.product.dto.ProductResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public PageResponse<ProductListResponse> findAll(Pageable pageable) {
        return PageResponse.of(productRepository.findAll(pageable).map(ProductListResponse::from));
    }

    @Override
    public ProductResponse findById(UUID id) {
        return ProductResponse.from(productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Product not found")));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CONFLICT, "Slug already in use");
        }
        Product product = new Product();
        mapRequestToEntity(request, product);
        return ProductResponse.from(productRepository.save(product));
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
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
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
