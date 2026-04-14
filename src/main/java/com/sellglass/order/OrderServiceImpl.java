package com.sellglass.order;

import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import com.sellglass.order.dto.OrderRequest;
import com.sellglass.order.dto.OrderResponse;
import com.sellglass.order.dto.OrderStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    public PageResponse<OrderResponse> findByCustomer(UUID customerId, Pageable pageable) {
        return PageResponse.of(orderRepository.findByCustomerId(customerId, pageable).map(OrderResponse::from));
    }

    @Override
    public PageResponse<OrderResponse> findAll(Pageable pageable) {
        return PageResponse.of(orderRepository.findAll(pageable).map(OrderResponse::from));
    }

    @Override
    public OrderResponse findById(UUID id) {
        return OrderResponse.from(orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order not found")));
    }

    @Override
    @Transactional
    public OrderResponse create(UUID customerId, OrderRequest request) {
        // TODO: Validate branch exists
        // TODO: Apply shipping fee logic based on orderType

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantRepository.findById(itemReq.getProductVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Variant not found: " + itemReq.getProductVariantId()));

            BigDecimal itemSubtotal = variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            OrderItem item = new OrderItem();
            item.setProductVariantId(variant.getId());
            item.setProductName(""); // TODO: fetch from product
            item.setVariantSku(variant.getSku());
            item.setUnitPrice(variant.getPrice());
            item.setQuantity(itemReq.getQuantity());
            item.setSubtotal(itemSubtotal);
            items.add(item);
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        // TODO: compute shippingFee based on orderType == DELIVERY

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setBranchId(request.getBranchId());
        order.setOrderType(request.getOrderType());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotal(subtotal.add(shippingFee));
        order.setNote(request.getNote());
        order = orderRepository.save(order);

        final UUID savedOrderId = order.getId();
        items.forEach(item -> item.setOrderId(savedOrderId));
        orderItemRepository.saveAll(items);

        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order not found"));
        order.setStatus(request.getStatus());
        if (request.getCancelledReason() != null) {
            order.setCancelledReason(request.getCancelledReason());
        }
        return OrderResponse.from(orderRepository.save(order));
    }
}
