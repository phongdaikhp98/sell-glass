package com.sellglass.review;

import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.order.OrderItemRepository;
import com.sellglass.review.dto.ReviewRequest;
import com.sellglass.review.dto.ReviewResponse;
import com.sellglass.customer.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public PageResponse<ReviewResponse> findByProduct(UUID productId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductId(productId, pageable);

        Set<UUID> customerIds = page.getContent().stream()
                .map(Review::getCustomerId)
                .collect(Collectors.toSet());
        Map<UUID, String> nameMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getFullName));

        return PageResponse.of(page.map(r -> ReviewResponse.from(r, nameMap.getOrDefault(r.getCustomerId(), "Khách hàng"))));
    }

    @Override
    @Transactional
    public ReviewResponse create(UUID customerId, UUID productId, ReviewRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Sản phẩm không tồn tại");
        }
        if (!orderItemRepository.customerHasCompletedOrderForProduct(customerId, productId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Bạn cần mua và nhận sản phẩm này trước khi đánh giá");
        }
        if (reviewRepository.existsByCustomerIdAndProductId(customerId, productId)) {
            throw new AppException(ErrorCode.CONFLICT, "Bạn đã đánh giá sản phẩm này rồi");
        }

        Review review = new Review();
        review.setCustomerId(customerId);
        review.setProductId(productId);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);

        String customerName = customerRepository.findById(customerId)
                .map(Customer::getFullName)
                .orElse("Khách hàng");

        return ReviewResponse.from(review, customerName);
    }

    @Override
    @Transactional
    public void delete(UUID reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Review not found");
        }
        reviewRepository.deleteById(reviewId);
    }
}
