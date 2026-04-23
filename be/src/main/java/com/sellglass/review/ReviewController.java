package com.sellglass.review;

import com.sellglass.common.response.ApiResponse;
import com.sellglass.common.response.PageResponse;
import com.sellglass.review.dto.ReviewRequest;
import com.sellglass.review.dto.ReviewResponse;
import com.sellglass.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/v1/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> list(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(reviewService.findByProduct(productId, pageable)));
    }

    @PostMapping("/v1/products/{productId}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.create(userDetails.getUserId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/v1/admin/reviews/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
