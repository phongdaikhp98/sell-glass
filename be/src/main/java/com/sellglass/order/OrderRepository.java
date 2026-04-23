package com.sellglass.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByBranchId(UUID branchId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status <> com.sellglass.order.OrderStatus.CANCELLED")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status <> com.sellglass.order.OrderStatus.CANCELLED AND o.branchId = :branchId")
    BigDecimal sumTotalRevenueByBranch(@Param("branchId") UUID branchId);

    @Query(value = "SELECT TO_CHAR(o.created_at, 'YYYY-MM-DD') as date, SUM(o.total) as revenue FROM orders o WHERE o.status != 'CANCELLED' AND o.created_at >= :since GROUP BY TO_CHAR(o.created_at, 'YYYY-MM-DD') ORDER BY date", nativeQuery = true)
    List<Object[]> revenueByDay(@Param("since") LocalDateTime since);

    @Query(value = "SELECT TO_CHAR(o.created_at, 'YYYY-MM') as date, SUM(o.total) as revenue FROM orders o WHERE o.status != 'CANCELLED' AND o.created_at >= :since GROUP BY TO_CHAR(o.created_at, 'YYYY-MM') ORDER BY date", nativeQuery = true)
    List<Object[]> revenueByMonth(@Param("since") LocalDateTime since);

    @Query(value = "SELECT TO_CHAR(o.created_at, 'YYYY-MM-DD') as date, SUM(o.total) as revenue FROM orders o WHERE o.status != 'CANCELLED' AND o.created_at >= :since AND o.branch_id = :branchId GROUP BY TO_CHAR(o.created_at, 'YYYY-MM-DD') ORDER BY date", nativeQuery = true)
    List<Object[]> revenueByDayAndBranch(@Param("since") LocalDateTime since, @Param("branchId") UUID branchId);

    @Query(value = "SELECT TO_CHAR(o.created_at, 'YYYY-MM') as date, SUM(o.total) as revenue FROM orders o WHERE o.status != 'CANCELLED' AND o.created_at >= :since AND o.branch_id = :branchId GROUP BY TO_CHAR(o.created_at, 'YYYY-MM') ORDER BY date", nativeQuery = true)
    List<Object[]> revenueByMonthAndBranch(@Param("since") LocalDateTime since, @Param("branchId") UUID branchId);

    @Query(value = "SELECT o.status, COUNT(*) FROM orders o GROUP BY o.status", nativeQuery = true)
    List<Object[]> countByStatus();

    @Query(value = """
            SELECT * FROM orders
            WHERE LOWER(LEFT(id::text, 8)) = :suffix
              AND payment_status != 'PAID'
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Order> findUnpaidByOrderCodeSuffix(@Param("suffix") String suffix);
}
