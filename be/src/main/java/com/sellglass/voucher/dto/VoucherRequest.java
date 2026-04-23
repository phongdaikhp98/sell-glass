package com.sellglass.voucher.dto;

import com.sellglass.voucher.VoucherType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class VoucherRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Type is required")
    private VoucherType type;

    @NotNull(message = "Value is required")
    @Positive(message = "Value must be positive")
    private BigDecimal value;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    private Integer usageLimit;

    private LocalDateTime expiresAt;

    private boolean isActive = true;
}
