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

    private String voucherCode;

    private PrescriptionRequest prescription;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemRequest {
        @NotNull(message = "Product variant ID is required")
        private UUID productVariantId;
        private int quantity = 1;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PrescriptionRequest {
        private java.math.BigDecimal odSph;
        private java.math.BigDecimal odCyl;
        private Integer odAxis;
        private java.math.BigDecimal osSph;
        private java.math.BigDecimal osCyl;
        private Integer osAxis;
        private java.math.BigDecimal pd;
        private String note;
    }
}
