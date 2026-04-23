package com.sellglass.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductId(UUID productId, Pageable pageable);

    Optional<Review> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    boolean existsByCustomerIdAndProductId(UUID customerId, UUID productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Double avgRatingByProductId(@Param("productId") UUID productId);

    long countByProductId(UUID productId);
}
