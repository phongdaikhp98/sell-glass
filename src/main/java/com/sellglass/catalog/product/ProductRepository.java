package com.sellglass.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByBrandId(UUID brandId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);
}
