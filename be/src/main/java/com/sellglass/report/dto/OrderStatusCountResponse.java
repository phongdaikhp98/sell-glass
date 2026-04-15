package com.sellglass.report.dto;

import com.sellglass.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderStatusCountResponse {

    private OrderStatus status;
    private long count;
}
