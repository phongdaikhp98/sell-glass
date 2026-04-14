package com.sellglass.order.dto;

import com.sellglass.order.Order;
import com.sellglass.order.OrderStatus;
import com.sellglass.order.OrderType;
import com.sellglass.order.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID customerId;
    private UUID branchId;
    private OrderType orderType;
    private OrderStatus status;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private PaymentStatus paymentStatus;
    private String note;
    private String cancelledReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        OrderResponse response = new OrderResponse();
        response.id = order.getId();
        response.customerId = order.getCustomerId();
        response.branchId = order.getBranchId();
        response.orderType = order.getOrderType();
        response.status = order.getStatus();
        response.receiverName = order.getReceiverName();
        response.receiverPhone = order.getReceiverPhone();
        response.deliveryAddress = order.getDeliveryAddress();
        response.subtotal = order.getSubtotal();
        response.shippingFee = order.getShippingFee();
        response.total = order.getTotal();
        response.paymentStatus = order.getPaymentStatus();
        response.note = order.getNote();
        response.cancelledReason = order.getCancelledReason();
        response.createdAt = order.getCreatedAt();
        response.updatedAt = order.getUpdatedAt();
        return response;
    }
}
