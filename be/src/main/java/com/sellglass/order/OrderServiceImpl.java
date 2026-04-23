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
import com.sellglass.voucher.VoucherService;
import com.sellglass.voucher.dto.ApplyVoucherResponse;
import com.sellglass.order.dto.OrderRequest;
import com.sellglass.order.dto.OrderResponse;
import com.sellglass.order.dto.OrderStatusRequest;
import com.sellglass.order.dto.PaymentStatusRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final VoucherService voucherService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

        // Check stock before building items
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantMap.get(itemReq.getProductVariantId());
            if (variant == null) {
                throw new AppException(ErrorCode.NOT_FOUND, "Variant not found: " + itemReq.getProductVariantId());
            }
            if (variant.getStock() < itemReq.getQuantity()) {
                String name = productNameMap.getOrDefault(variant.getProductId(), variant.getSku());
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Sản phẩm \"" + name + "\" không đủ hàng. Tồn kho: " + variant.getStock());
            }
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        List<ProductVariant> variantsToDeduct = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantMap.get(itemReq.getProductVariantId());

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

            variant.setStock(variant.getStock() - itemReq.getQuantity());
            variantsToDeduct.add(variant);
        }

        BigDecimal shippingFee = request.getOrderType() == OrderType.DELIVERY
                ? new BigDecimal("30000")
                : BigDecimal.ZERO;

        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedVoucherCode = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            String code = request.getVoucherCode().trim().toUpperCase();
            ApplyVoucherResponse applied = voucherService.apply(code, subtotal.add(shippingFee));
            discountAmount = applied.getDiscountAmount();
            appliedVoucherCode = code;
        }

        BigDecimal total = subtotal.add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO);

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setBranchId(request.getBranchId());
        order.setOrderType(request.getOrderType());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setTotal(total);
        order.setVoucherCode(appliedVoucherCode);
        order.setNote(request.getNote());

        if (request.getPrescription() != null) {
            OrderRequest.PrescriptionRequest p = request.getPrescription();
            order.setPrescriptionOdSph(p.getOdSph());
            order.setPrescriptionOdCyl(p.getOdCyl());
            order.setPrescriptionOdAxis(p.getOdAxis());
            order.setPrescriptionOsSph(p.getOsSph());
            order.setPrescriptionOsCyl(p.getOsCyl());
            order.setPrescriptionOsAxis(p.getOsAxis());
            order.setPrescriptionPd(p.getPd());
            order.setPrescriptionNote(p.getNote());
        }

        final Order savedOrder = orderRepository.save(order);

        final UUID savedOrderId = savedOrder.getId();
        items.forEach(item -> item.setOrderId(savedOrderId));
        orderItemRepository.saveAll(items);
        variantRepository.saveAll(variantsToDeduct);

        if (appliedVoucherCode != null) {
            voucherService.incrementUsage(appliedVoucherCode);
        }

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
                                "total", savedOrder.getTotal(),
                                "items", items,
                                "transferContent", orderCode,
                                "bankInfo", "Vietcombank — 1234567890 — SELL GLASS"
                        )
                );
            });
        } catch (Exception e) {
            log.warn("Failed to send order confirmation email: {}", e.getMessage());
        }

        return OrderResponse.from(savedOrder, items, branch.getName());
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

        final Order finalOrder = order;
        try {
            customerRepository.findById(order.getCustomerId()).ifPresent(customer -> {
                String orderCode = "SG-" + finalOrder.getId().toString().substring(0, 8).toUpperCase();
                Map<String, Object> vars = new HashMap<>();
                vars.put("fullName", customer.getFullName());
                vars.put("orderCode", orderCode);
                vars.put("statusLabel", getStatusLabel(finalOrder.getStatus()));
                vars.put("statusMessage", getStatusMessage(finalOrder.getStatus()));
                vars.put("cancelledReason", finalOrder.getCancelledReason());
                vars.put("orderUrl", frontendUrl + "/orders/" + finalOrder.getId());
                mailService.send(
                        customer.getEmail(),
                        "[Sell Glass] Đơn " + orderCode + " — " + getStatusLabel(finalOrder.getStatus()),
                        "emails/order-status-update",
                        vars
                );
            });
        } catch (Exception e) {
            log.warn("Failed to send status update email: {}", e.getMessage());
        }

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

    private String getStatusLabel(OrderStatus status) {
        return switch (status) {
            case PENDING    -> "Chờ xác nhận";
            case CONFIRMED  -> "Đã xác nhận";
            case PROCESSING -> "Đang xử lý";
            case READY      -> "Sẵn sàng nhận hàng";
            case DELIVERING -> "Đang giao hàng";
            case COMPLETED  -> "Hoàn thành";
            case CANCELLED  -> "Đã hủy";
        };
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case PENDING    -> "Đơn hàng của bạn đang chờ xác nhận từ nhân viên.";
            case CONFIRMED  -> "Đơn hàng của bạn đã được xác nhận và sẽ sớm được xử lý.";
            case PROCESSING -> "Nhân viên đang thực hiện đơn hàng của bạn.";
            case READY      -> "Đơn hàng đã sẵn sàng. Bạn có thể đến cửa hàng để nhận.";
            case DELIVERING -> "Đơn hàng đang trên đường giao đến bạn.";
            case COMPLETED  -> "Đơn hàng đã hoàn thành. Cảm ơn bạn đã mua hàng tại Sell Glass!";
            case CANCELLED  -> "Đơn hàng của bạn đã bị hủy.";
        };
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
