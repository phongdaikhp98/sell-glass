package com.sellglass.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ApplyVoucherRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Order total is required")
    @Positive(message = "Order total must be positive")
    private BigDecimal orderTotal;
}
