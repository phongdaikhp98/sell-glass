package com.sellglass.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SummaryResponse {

    private long totalOrders;
    private BigDecimal totalRevenue;
    private long totalCustomers;
    private long totalProducts;
}
