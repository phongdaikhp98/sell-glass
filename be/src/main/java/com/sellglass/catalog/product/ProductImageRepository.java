package com.sellglass.catalog.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdIn(Collection<UUID> productIds);

    List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);
}
