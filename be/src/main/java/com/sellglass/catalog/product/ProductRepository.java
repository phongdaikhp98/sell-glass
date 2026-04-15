package com.sellglass.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByBrandId(UUID brandId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
            AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:categoryId IS NULL OR p.categoryId = :categoryId)
            AND (:brandId IS NULL OR p.brandId = :brandId)
            AND (:gender IS NULL OR p.gender = :gender)
            """)
    Page<Product> findWithFilters(
            @Param("search") String search,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            @Param("gender") Product.Gender gender,
            Pageable pageable
    );
}
