package com.sellglass.order.dto;

import com.sellglass.order.Order;
import com.sellglass.order.OrderItem;
import com.sellglass.order.OrderStatus;
import com.sellglass.order.OrderType;
import com.sellglass.order.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID customerId;
    private UUID branchId;
    private String branchName;
    private OrderType orderType;
    private OrderStatus status;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String voucherCode;
    private PaymentStatus paymentStatus;
    private String note;
    private String cancelledReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDetail> items;
    private PrescriptionInfo prescription;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemDetail {
        private UUID id;
        private String productName;
        private String variantSku;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal subtotal;

        public static OrderItemDetail from(OrderItem item) {
            OrderItemDetail detail = new OrderItemDetail();
            detail.id = item.getId();
            detail.productName = item.getProductName();
            detail.variantSku = item.getVariantSku();
            detail.unitPrice = item.getUnitPrice();
            detail.quantity = item.getQuantity();
            detail.subtotal = item.getSubtotal();
            return detail;
        }
    }

    public static OrderResponse from(Order order, List<OrderItem> items, String branchName) {
        OrderResponse response = new OrderResponse();
        response.id = order.getId();
        response.customerId = order.getCustomerId();
        response.branchId = order.getBranchId();
        response.branchName = branchName;
        response.orderType = order.getOrderType();
        response.status = order.getStatus();
        response.receiverName = order.getReceiverName();
        response.receiverPhone = order.getReceiverPhone();
        response.deliveryAddress = order.getDeliveryAddress();
        response.subtotal = order.getSubtotal();
        response.shippingFee = order.getShippingFee();
        response.discountAmount = order.getDiscountAmount();
        response.total = order.getTotal();
        response.voucherCode = order.getVoucherCode();
        response.paymentStatus = order.getPaymentStatus();
        response.note = order.getNote();
        response.cancelledReason = order.getCancelledReason();
        response.createdAt = order.getCreatedAt();
        response.updatedAt = order.getUpdatedAt();
        response.items = items.stream().map(OrderItemDetail::from).toList();
        response.prescription = PrescriptionInfo.from(order);
        return response;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PrescriptionInfo {
        private java.math.BigDecimal odSph;
        private java.math.BigDecimal odCyl;
        private Integer odAxis;
        private java.math.BigDecimal osSph;
        private java.math.BigDecimal osCyl;
        private Integer osAxis;
        private java.math.BigDecimal pd;
        private String note;

        public static PrescriptionInfo from(Order order) {
            if (order.getPrescriptionOdSph() == null && order.getPrescriptionOsSph() == null
                    && order.getPrescriptionPd() == null) {
                return null;
            }
            PrescriptionInfo info = new PrescriptionInfo();
            info.odSph = order.getPrescriptionOdSph();
            info.odCyl = order.getPrescriptionOdCyl();
            info.odAxis = order.getPrescriptionOdAxis();
            info.osSph = order.getPrescriptionOsSph();
            info.osCyl = order.getPrescriptionOsCyl();
            info.osAxis = order.getPrescriptionOsAxis();
            info.pd = order.getPrescriptionPd();
            info.note = order.getPrescriptionNote();
            return info;
        }
    }
}
