package com.sellglass.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TopProductResponse {

    private String productName;
    private long totalQuantity;
    private BigDecimal totalRevenue;
}
