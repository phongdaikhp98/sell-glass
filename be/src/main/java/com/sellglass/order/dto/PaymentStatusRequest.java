package com.sellglass.order.dto;

import com.sellglass.order.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentStatusRequest {
    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus;
}
