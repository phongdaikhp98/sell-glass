package com.sellglass.catalog.variant;

import com.sellglass.catalog.variant.dto.ProductVariantRequest;
import com.sellglass.catalog.variant.dto.ProductVariantResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository variantRepository;

    @Override
    public List<ProductVariantResponse> findByProductId(UUID productId) {
        return variantRepository.findByProductId(productId).stream()
                .map(ProductVariantResponse::from)
                .toList();
    }

    @Override
    public ProductVariantResponse findById(UUID id) {
        return ProductVariantResponse.from(variantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Variant not found")));
    }

    @Override
    @Transactional
    public ProductVariantResponse create(UUID productId, ProductVariantRequest request) {
        if (variantRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.CONFLICT, "SKU already in use");
        }
        ProductVariant variant = new ProductVariant();
        variant.setProductId(productId);
        mapRequestToEntity(request, variant);
        return ProductVariantResponse.from(variantRepository.save(variant));
    }

    @Override
    @Transactional
    public ProductVariantResponse update(UUID id, ProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Variant not found"));
        if (!variant.getSku().equals(request.getSku()) && variantRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.CONFLICT, "SKU already in use");
        }
        mapRequestToEntity(request, variant);
        return ProductVariantResponse.from(variantRepository.save(variant));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!variantRepository.existsById(id)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Variant not found");
        }
        variantRepository.deleteById(id);
    }

    private void mapRequestToEntity(ProductVariantRequest request, ProductVariant variant) {
        variant.setSku(request.getSku());
        variant.setColor(request.getColor());
        variant.setSize(request.getSize());
        variant.setPrice(request.getPrice());
        variant.setActive(request.isActive());
    }
}
