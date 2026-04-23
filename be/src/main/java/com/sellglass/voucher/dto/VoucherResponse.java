package com.sellglass.voucher.dto;

import com.sellglass.voucher.Voucher;
import com.sellglass.voucher.VoucherType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class VoucherResponse {

    private UUID id;
    private String code;
    private VoucherType type;
    private BigDecimal value;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private int timesUsed;
    private LocalDateTime expiresAt;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static VoucherResponse from(Voucher v) {
        VoucherResponse r = new VoucherResponse();
        r.id = v.getId();
        r.code = v.getCode();
        r.type = v.getType();
        r.value = v.getValue();
        r.maxDiscountAmount = v.getMaxDiscountAmount();
        r.minOrderAmount = v.getMinOrderAmount();
        r.usageLimit = v.getUsageLimit();
        r.timesUsed = v.getTimesUsed();
        r.expiresAt = v.getExpiresAt();
        r.isActive = v.isActive();
        r.createdAt = v.getCreatedAt();
        return r;
    }
}
