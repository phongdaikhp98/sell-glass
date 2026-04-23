package com.sellglass.review;

import com.sellglass.common.response.PageResponse;
import com.sellglass.review.dto.ReviewRequest;
import com.sellglass.review.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    PageResponse<ReviewResponse> findByProduct(UUID productId, Pageable pageable);

    ReviewResponse create(UUID customerId, UUID productId, ReviewRequest request);

    void delete(UUID reviewId);
}
