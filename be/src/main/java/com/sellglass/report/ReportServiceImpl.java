package com.sellglass.report;

import com.sellglass.customer.CustomerRepository;
import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.order.OrderItemRepository;
import com.sellglass.order.OrderRepository;
import com.sellglass.order.OrderStatus;
import com.sellglass.report.dto.OrderStatusCountResponse;
import com.sellglass.report.dto.RevenueResponse;
import com.sellglass.report.dto.SummaryResponse;
import com.sellglass.report.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    public SummaryResponse getSummary() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long totalCustomers = customerRepository.count();
        long totalProducts = productRepository.count();
        return new SummaryResponse(totalOrders, totalRevenue, totalCustomers, totalProducts);
    }

    @Override
    public List<RevenueResponse> getRevenue(String period, UUID branchId) {
        List<Object[]> rows;
        if ("day".equals(period)) {
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            rows = branchId != null
                    ? orderRepository.revenueByDayAndBranch(since, branchId)
                    : orderRepository.revenueByDay(since);
        } else {
            LocalDateTime since = LocalDateTime.now().minusMonths(12);
            rows = branchId != null
                    ? orderRepository.revenueByMonthAndBranch(since, branchId)
                    : orderRepository.revenueByMonth(since);
        }
        return rows.stream()
                .map(row -> new RevenueResponse(
                        (String) row[0],
                        row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Override
    public List<TopProductResponse> getTopProducts(int limit) {
        return orderItemRepository.topProducts(limit).stream()
                .map(row -> new TopProductResponse(
                        (String) row[0],
                        row[1] != null ? ((Number) row[1]).longValue() : 0L,
                        row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderStatusCountResponse> getOrdersByStatus() {
        return orderRepository.countByStatus().stream()
                .map(row -> new OrderStatusCountResponse(
                        OrderStatus.valueOf((String) row[0]),
                        row[1] != null ? ((Number) row[1]).longValue() : 0L))
                .collect(Collectors.toList());
    }
}
