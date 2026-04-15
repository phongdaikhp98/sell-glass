package com.sellglass.report;

import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.order.OrderItemRepository;
import com.sellglass.order.OrderRepository;
import com.sellglass.order.OrderStatus;
import com.sellglass.report.dto.OrderStatusCountResponse;
import com.sellglass.report.dto.RevenueResponse;
import com.sellglass.report.dto.SummaryResponse;
import com.sellglass.report.dto.TopProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private ReportServiceImpl service;

    private static List<Object[]> rows(Object[]... rows) {
        List<Object[]> list = new ArrayList<>();
        for (Object[] row : rows) {
            list.add(row);
        }
        return list;
    }

    @Test
    @DisplayName("getSummary should aggregate counts and revenue")
    void getSummary_success() {
        when(orderRepository.count()).thenReturn(50L);
        when(orderRepository.sumTotalRevenue()).thenReturn(new BigDecimal("25000000"));
        when(customerRepository.count()).thenReturn(30L);
        when(productRepository.count()).thenReturn(100L);

        SummaryResponse result = service.getSummary();

        assertThat(result.getTotalOrders()).isEqualTo(50L);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("25000000"));
        assertThat(result.getTotalCustomers()).isEqualTo(30L);
        assertThat(result.getTotalProducts()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getRevenue by day should call revenueByDay without branchId filter")
    void getRevenue_byDay_noBranch() {
        Object[] row = {"2025-04-01", new BigDecimal("500000")};
        when(orderRepository.revenueByDay(any())).thenReturn(rows(row));

        List<RevenueResponse> result = service.getRevenue("day", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo("2025-04-01");
        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(new BigDecimal("500000"));
        verify(orderRepository).revenueByDay(any());
    }

    @Test
    @DisplayName("getRevenue by day with branchId should call revenueByDayAndBranch")
    void getRevenue_byDay_withBranch() {
        UUID branchId = UUID.randomUUID();
        Object[] row = {"2025-04-01", new BigDecimal("300000")};
        when(orderRepository.revenueByDayAndBranch(any(), any())).thenReturn(rows(row));

        List<RevenueResponse> result = service.getRevenue("day", branchId);

        assertThat(result).hasSize(1);
        verify(orderRepository).revenueByDayAndBranch(any(), any());
    }

    @Test
    @DisplayName("getRevenue by month should call revenueByMonth")
    void getRevenue_byMonth_noBranch() {
        Object[] row = {"2025-04", new BigDecimal("5000000")};
        when(orderRepository.revenueByMonth(any())).thenReturn(rows(row));

        List<RevenueResponse> result = service.getRevenue("month", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo("2025-04");
        verify(orderRepository).revenueByMonth(any());
    }

    @Test
    @DisplayName("getRevenue should handle null revenue value as ZERO")
    void getRevenue_nullRevenue_returnsZero() {
        Object[] row = {"2025-04-01", null};
        when(orderRepository.revenueByDay(any())).thenReturn(rows(row));

        List<RevenueResponse> result = service.getRevenue("day", null);

        assertThat(result.get(0).getRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getTopProducts should return top selling products")
    void getTopProducts_success() {
        Object[] row = {"Kính Ray-Ban", 25L, new BigDecimal("12500000")};
        when(orderItemRepository.topProducts(5)).thenReturn(rows(row));

        List<TopProductResponse> result = service.getTopProducts(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductName()).isEqualTo("Kính Ray-Ban");
        assertThat(result.get(0).getTotalQuantity()).isEqualTo(25L);
        assertThat(result.get(0).getTotalRevenue()).isEqualByComparingTo(new BigDecimal("12500000"));
    }

    @Test
    @DisplayName("getOrdersByStatus should return count per status")
    void getOrdersByStatus_success() {
        Object[] row1 = {"PENDING", 10L};
        Object[] row2 = {"COMPLETED", 40L};
        when(orderRepository.countByStatus()).thenReturn(rows(row1, row2));

        List<OrderStatusCountResponse> result = service.getOrdersByStatus();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get(0).getCount()).isEqualTo(10L);
        assertThat(result.get(1).getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(result.get(1).getCount()).isEqualTo(40L);
    }

    @Test
    @DisplayName("getTopProducts should handle null quantity/revenue as 0")
    void getTopProducts_nullValues() {
        Object[] row = {"Kính X", null, null};
        when(orderItemRepository.topProducts(3)).thenReturn(rows(row));

        List<TopProductResponse> result = service.getTopProducts(3);

        assertThat(result.get(0).getTotalQuantity()).isEqualTo(0L);
        assertThat(result.get(0).getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
