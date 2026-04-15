package com.sellglass.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RevenueResponse {

    private String date;
    private BigDecimal revenue;
}
