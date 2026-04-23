package com.sellglass.report;

import com.sellglass.common.response.ApiResponse;
import com.sellglass.report.dto.OrderStatusCountResponse;
import com.sellglass.report.dto.RevenueResponse;
import com.sellglass.report.dto.SummaryResponse;
import com.sellglass.report.dto.TopProductResponse;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/reports")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary()));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<List<RevenueResponse>>> getRevenue(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) UUID branchId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getRevenue(period, branchId)));
    }

    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @Max(value = 100, message = "Limit cannot exceed 100") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getTopProducts(limit)));
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<ApiResponse<List<OrderStatusCountResponse>>> getOrdersByStatus() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getOrdersByStatus()));
    }
}
