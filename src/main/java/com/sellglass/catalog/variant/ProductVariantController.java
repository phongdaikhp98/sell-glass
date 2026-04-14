package com.sellglass.catalog.variant;

import com.sellglass.catalog.variant.dto.ProductVariantRequest;
import com.sellglass.catalog.variant.dto.ProductVariantResponse;
import com.sellglass.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getByProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(productVariantService.findByProductId(productId)));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> getById(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        return ResponseEntity.ok(ApiResponse.success(productVariantService.findById(variantId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> create(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productVariantService.create(productId, request)));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> update(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productVariantService.update(variantId, request)));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        productVariantService.delete(variantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
