package com.sellglass.order;

import com.sellglass.common.response.PageResponse;
import com.sellglass.order.dto.OrderRequest;
import com.sellglass.order.dto.OrderResponse;
import com.sellglass.order.dto.OrderStatusRequest;
import com.sellglass.order.dto.PaymentStatusRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    PageResponse<OrderResponse> findByCustomer(UUID customerId, Pageable pageable);

    PageResponse<OrderResponse> findAll(Pageable pageable);

    OrderResponse findById(UUID id);

    OrderResponse create(UUID customerId, OrderRequest request);

    OrderResponse updateStatus(UUID orderId, OrderStatusRequest request);

    OrderResponse updatePaymentStatus(UUID orderId, PaymentStatusRequest request);
}
