package com.sellglass.order;

import com.sellglass.branch.BranchRepository;
import com.sellglass.catalog.product.Product;
import com.sellglass.catalog.product.ProductRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

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
        branchRepository.findById(request.getBranchId())
                .filter(com.sellglass.branch.Branch::isActive)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Branch not found or inactive"));

        if (request.getOrderType() == OrderType.DELIVERY) {
            if (request.getReceiverName() == null || request.getReceiverPhone() == null || request.getDeliveryAddress() == null) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Delivery order requires receiver name, phone and address");
            }
        }

        List<UUID> variantIds = request.getItems().stream()
                .map(OrderRequest.OrderItemRequest::getProductVariantId)
                .collect(Collectors.toList());
        List<ProductVariant> variants = variantRepository.findAllById(variantIds);
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        Set<UUID> productIds = variants.stream()
                .map(ProductVariant::getProductId)
                .collect(Collectors.toSet());
        Map<UUID, String> productNameMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantMap.get(itemReq.getProductVariantId());
            if (variant == null) {
                throw new AppException(ErrorCode.NOT_FOUND, "Variant not found: " + itemReq.getProductVariantId());
            }

            String productName = productNameMap.get(variant.getProductId());
            if (productName == null) {
                throw new AppException(ErrorCode.NOT_FOUND, "Product not found");
            }

            BigDecimal itemSubtotal = variant.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(itemSubtotal);

            OrderItem item = new OrderItem();
            item.setProductVariantId(variant.getId());
            item.setProductName(productName);
            item.setVariantSku(variant.getSku());
            item.setUnitPrice(variant.getPrice());
            item.setQuantity(itemReq.getQuantity());
            item.setSubtotal(itemSubtotal);
            items.add(item);
        }

        BigDecimal shippingFee = request.getOrderType() == OrderType.DELIVERY
                ? new BigDecimal("30000")
                : BigDecimal.ZERO;

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
