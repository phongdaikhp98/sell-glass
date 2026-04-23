package com.sellglass.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findByOrderIdIn(Collection<UUID> orderIds);

    @Query(value = "SELECT oi.product_name, SUM(oi.quantity) as total_qty, SUM(oi.subtotal) as total_rev FROM order_items oi JOIN orders o ON oi.order_id = o.id WHERE o.status != 'CANCELLED' GROUP BY oi.product_name ORDER BY total_rev DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> topProducts(@Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(*) > 0 FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            JOIN product_variants pv ON oi.product_variant_id = pv.id
            WHERE o.customer_id = :customerId
              AND o.status = 'COMPLETED'
              AND pv.product_id = :productId
            """, nativeQuery = true)
    boolean customerHasCompletedOrderForProduct(
            @Param("customerId") UUID customerId,
            @Param("productId") UUID productId);
}
