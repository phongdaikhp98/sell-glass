package com.sellglass.order.dto;

import com.sellglass.order.OrderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "Branch ID is required")
    private UUID branchId;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;

    private String note;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product variant ID is required")
        private UUID productVariantId;
        private int quantity = 1;
    }
}
