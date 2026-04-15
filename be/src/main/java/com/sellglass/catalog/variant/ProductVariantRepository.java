package com.sellglass.catalog.variant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductId(UUID productId);

    List<ProductVariant> findByProductIdIn(Collection<UUID> productIds);

    List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);
}
