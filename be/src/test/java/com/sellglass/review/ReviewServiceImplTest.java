package com.sellglass.review;

import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.customer.Customer;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.order.OrderItemRepository;
import com.sellglass.review.dto.ReviewRequest;
import com.sellglass.review.dto.ReviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl service;

    private UUID customerId;
    private UUID productId;
    private UUID reviewId;
    private Customer customer;
    private Review review;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId  = UUID.randomUUID();
        reviewId   = UUID.randomUUID();
        pageable   = PageRequest.of(0, 5);

        customer = new Customer();
        customer.setId(customerId);
        customer.setFullName("Nguyen Van A");

        review = new Review();
        review.setId(reviewId);
        review.setCustomerId(customerId);
        review.setProductId(productId);
        review.setRating((short) 5);
        review.setComment("Great product!");
    }

    // ─── findByProduct ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByProduct should return reviews enriched with customer names")
    void findByProduct_success() {
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByProductId(productId, pageable)).thenReturn(page);
        when(customerRepository.findAllById(any())).thenReturn(List.of(customer));

        var result = service.findByProduct(productId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("Nguyen Van A");
        assertThat(result.getContent().get(0).getRating()).isEqualTo((short) 5);
        assertThat(result.getContent().get(0).getComment()).isEqualTo("Great product!");
    }

    @Test
    @DisplayName("findByProduct should return empty page and use fallback name for unknown customer")
    void findByProduct_unknownCustomerFallback() {
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByProductId(productId, pageable)).thenReturn(page);
        when(customerRepository.findAllById(any())).thenReturn(List.of()); // customer not found

        var result = service.findByProduct(productId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("Khách hàng");
    }

    @Test
    @DisplayName("findByProduct should return empty page when product has no reviews")
    void findByProduct_empty() {
        when(reviewRepository.findByProductId(productId, pageable))
                .thenReturn(new PageImpl<>(List.of()));
        when(customerRepository.findAllById(any())).thenReturn(List.of());

        var result = service.findByProduct(productId, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create should save review when all validations pass")
    void create_success() {
        ReviewRequest request = new ReviewRequest();
        request.setRating((short) 5);
        request.setComment("Excellent!");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(orderItemRepository.customerHasCompletedOrderForProduct(customerId, productId)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(customerId, productId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            saved.setId(reviewId);
            return saved;
        });
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        ReviewResponse result = service.create(customerId, productId, request);

        assertThat(result.getRating()).isEqualTo((short) 5);
        assertThat(result.getComment()).isEqualTo("Excellent!");
        assertThat(result.getCustomerName()).isEqualTo("Nguyen Van A");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("create should use fallback name when customer not found after save")
    void create_customerNotFoundAfterSave_usesFallback() {
        ReviewRequest request = new ReviewRequest();
        request.setRating((short) 4);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(orderItemRepository.customerHasCompletedOrderForProduct(customerId, productId)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(customerId, productId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            saved.setId(reviewId);
            return saved;
        });
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        ReviewResponse result = service.create(customerId, productId, request);

        assertThat(result.getCustomerName()).isEqualTo("Khách hàng");
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when product does not exist")
    void create_productNotFound() {
        ReviewRequest request = new ReviewRequest();
        request.setRating((short) 4);

        when(productRepository.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(customerId, productId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw BAD_REQUEST when customer has no completed order for product")
    void create_noPurchase() {
        ReviewRequest request = new ReviewRequest();
        request.setRating((short) 3);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(orderItemRepository.customerHasCompletedOrderForProduct(customerId, productId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(customerId, productId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw CONFLICT when customer already reviewed this product")
    void create_alreadyReviewed() {
        ReviewRequest request = new ReviewRequest();
        request.setRating((short) 4);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(orderItemRepository.customerHasCompletedOrderForProduct(customerId, productId)).thenReturn(true);
        when(reviewRepository.existsByCustomerIdAndProductId(customerId, productId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(customerId, productId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(reviewRepository, never()).save(any());
    }

    // ─── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete should remove review when exists")
    void delete_success() {
        when(reviewRepository.existsById(reviewId)).thenReturn(true);

        service.delete(reviewId);

        verify(reviewRepository).deleteById(reviewId);
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when review missing")
    void delete_notFound() {
        when(reviewRepository.existsById(reviewId)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(reviewId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(reviewRepository, never()).deleteById(any());
    }
}
