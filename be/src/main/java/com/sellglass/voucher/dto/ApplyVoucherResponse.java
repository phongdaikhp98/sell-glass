package com.sellglass.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ApplyVoucherResponse {
    private BigDecimal discountAmount;
    private String description;
}
