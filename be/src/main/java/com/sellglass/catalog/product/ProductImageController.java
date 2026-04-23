package com.sellglass.catalog.product;

import com.sellglass.catalog.product.dto.ProductImageResponse;
import com.sellglass.catalog.product.dto.ReorderRequest;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.ApiResponse;
import com.sellglass.upload.UploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/products/{productId}/images")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
public class ProductImageController {

    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final UploadService uploadService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImages(
            @PathVariable UUID productId) {
        requireProduct(productId);
        List<ProductImageResponse> images = imageRepository
                .findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(ProductImageResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<ApiResponse<ProductImageResponse>> upload(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file) throws IOException {

        requireProduct(productId);
        validateFile(file);

        String url = uploadService.upload(file);

        List<ProductImage> existing = imageRepository.findByProductIdOrderBySortOrderAsc(productId);
        int nextSortOrder = existing.size();
        boolean isFirst = existing.isEmpty();

        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setUrl(url);
        image.setSortOrder(nextSortOrder);
        image.setPrimary(isFirst);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ProductImageResponse.from(imageRepository.save(image))));
    }

    @DeleteMapping("/{imageId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {

        requireProduct(productId);
        ProductImage image = requireImage(imageId, productId);
        boolean wasPrimary = image.isPrimary();

        imageRepository.delete(image);
        uploadService.delete(image.getUrl());

        // Re-assign primary to first remaining image
        if (wasPrimary) {
            imageRepository.findByProductIdOrderBySortOrderAsc(productId)
                    .stream().findFirst()
                    .ifPresent(first -> {
                        first.setPrimary(true);
                        imageRepository.save(first);
                    });
        }

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{imageId}/primary")
    @Transactional
    public ResponseEntity<ApiResponse<ProductImageResponse>> setPrimary(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {

        requireProduct(productId);
        requireImage(imageId, productId);

        List<ProductImage> all = imageRepository.findByProductIdOrderBySortOrderAsc(productId);
        all.forEach(img -> img.setPrimary(img.getId().equals(imageId)));
        imageRepository.saveAll(all);

        ProductImage updated = all.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow();

        return ResponseEntity.ok(ApiResponse.success(ProductImageResponse.from(updated)));
    }

    @PutMapping("/reorder")
    @Transactional
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> reorder(
            @PathVariable UUID productId,
            @Valid @RequestBody List<ReorderRequest> items) {

        requireProduct(productId);

        List<ProductImage> all = imageRepository.findByProductIdOrderBySortOrderAsc(productId);
        Map<UUID, ProductImage> imageMap = all.stream()
                .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        items.forEach(item -> {
            ProductImage img = imageMap.get(item.getId());
            if (img != null) img.setSortOrder(item.getSortOrder());
        });

        List<ProductImage> saved = imageRepository.saveAll(all);
        List<ProductImageResponse> result = saved.stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(ProductImageResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Product not found");
        }
    }

    private ProductImage requireImage(UUID imageId, UUID productId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Image not found"));
        if (!image.getProductId().equals(productId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Image does not belong to this product");
        }
        return image;
    }

    private void validateFile(MultipartFile file) throws java.io.IOException {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new AppException(ErrorCode.BAD_REQUEST, "File size exceeds 10 MB limit");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Only JPEG, PNG and WebP images are allowed");
        }
        // Verify magic bytes — Content-Type header is client-controlled and cannot be trusted alone
        byte[] header = file.getBytes();
        if (!isJpeg(header) && !isPng(header) && !isWebp(header)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "File content does not match an allowed image format");
        }
    }

    private static boolean isJpeg(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        return b.length >= 4
                && (b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 0x50
                && (b[2] & 0xFF) == 0x4E && (b[3] & 0xFF) == 0x47;
    }

    private static boolean isWebp(byte[] b) {
        // RIFF????WEBP
        return b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }
}
