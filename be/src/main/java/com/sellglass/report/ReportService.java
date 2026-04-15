package com.sellglass.report;

import com.sellglass.report.dto.OrderStatusCountResponse;
import com.sellglass.report.dto.RevenueResponse;
import com.sellglass.report.dto.SummaryResponse;
import com.sellglass.report.dto.TopProductResponse;

import java.util.List;
import java.util.UUID;

public interface ReportService {

    SummaryResponse getSummary();

    List<RevenueResponse> getRevenue(String period, UUID branchId);

    List<TopProductResponse> getTopProducts(int limit);

    List<OrderStatusCountResponse> getOrdersByStatus();
}
