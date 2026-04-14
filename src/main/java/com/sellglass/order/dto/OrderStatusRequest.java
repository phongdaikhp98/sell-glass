package com.sellglass.order.dto;

import com.sellglass.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String cancelledReason;
}
