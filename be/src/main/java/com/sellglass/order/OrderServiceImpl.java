package com.sellglass.order;

import com.sellglass.branch.Branch;
import com.sellglass.branch.BranchRepository;
import com.sellglass.catalog.product.Product;
import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.mail.MailService;
import com.sellglass.order.dto.OrderRequest;
import com.sellglass.order.dto.OrderResponse;
import com.sellglass.order.dto.OrderStatusRequest;
import com.sellglass.order.dto.PaymentStatusRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final MailService mailService;

    @Override
    public PageResponse<OrderResponse> findByCustomer(UUID customerId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByCustomerId(customerId, pageable);
        return PageResponse.of(orderPage.map(o -> enrichOrder(o, buildItemsMap(orderPage), buildBranchNameMap(orderPage))));
    }

    @Override
    public PageResponse<OrderResponse> findAll(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return PageResponse.of(orderPage.map(o -> enrichOrder(o, buildItemsMap(orderPage), buildBranchNameMap(orderPage))));
    }

    @Override
    public OrderResponse findById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order not found"));
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        String branchName = branchRepository.findById(order.getBranchId())
                .map(Branch::getName)
                .orElse(null);
        return OrderResponse.from(order, items, branchName);
    }

    @Override
    @Transactional
    public OrderResponse create(UUID customerId, OrderRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .filter(Branch::isActive)
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

        try {
            customerRepository.findById(customerId).ifPresent(customer -> {
                String orderCode = "SG-" + savedOrderId.toString().substring(0, 8).toUpperCase();
                mailService.send(
                        customer.getEmail(),
                        "Xác nhận đơn hàng " + orderCode + " — Sell Glass",
                        "emails/order-confirmation",
                        Map.of(
                                "fullName", customer.getFullName(),
                                "orderCode", orderCode,
                                "total", order.getTotal(),
                                "items", items,
                                "transferContent", orderCode,
                                "bankInfo", "Vietcombank — 1234567890 — SELL GLASS"
                        )
                );
            });
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email: {}", e.getMessage());
        }

        return OrderResponse.from(order, items, branch.getName());
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
        order = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        String branchName = branchRepository.findById(order.getBranchId())
                .map(Branch::getName)
                .orElse(null);
        return OrderResponse.from(order, items, branchName);
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(UUID orderId, PaymentStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Order not found"));
        order.setPaymentStatus(request.getPaymentStatus());
        Order saved = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(saved.getId());
        String branchName = branchRepository.findById(saved.getBranchId())
                .map(Branch::getName).orElse(null);
        return OrderResponse.from(saved, items, branchName);
    }

    private Map<UUID, List<OrderItem>> buildItemsMap(Page<Order> orderPage) {
        Set<UUID> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .collect(Collectors.toSet());
        return orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
    }

    private Map<UUID, String> buildBranchNameMap(Page<Order> orderPage) {
        Set<UUID> branchIds = orderPage.getContent().stream()
                .map(Order::getBranchId)
                .collect(Collectors.toSet());
        return branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));
    }

    private OrderResponse enrichOrder(Order order, Map<UUID, List<OrderItem>> itemsMap, Map<UUID, String> branchNameMap) {
        return OrderResponse.from(
                order,
                itemsMap.getOrDefault(order.getId(), List.of()),
                branchNameMap.get(order.getBranchId())
        );
    }
}
