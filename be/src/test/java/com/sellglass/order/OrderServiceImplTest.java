package com.sellglass.order;

import com.sellglass.branch.Branch;
import com.sellglass.branch.BranchRepository;
import com.sellglass.catalog.product.Product;
import com.sellglass.catalog.product.ProductRepository;
import com.sellglass.catalog.variant.ProductVariant;
import com.sellglass.catalog.variant.ProductVariantRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.customer.Customer;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.mail.MailService;
import com.sellglass.order.dto.OrderRequest;
import com.sellglass.voucher.VoucherService;
import com.sellglass.order.dto.OrderResponse;
import com.sellglass.order.dto.OrderStatusRequest;
import com.sellglass.order.dto.PaymentStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MailService mailService;
    @Mock private VoucherService voucherService;

    @InjectMocks
    private OrderServiceImpl service;

    private UUID customerId;
    private UUID branchId;
    private UUID variantId;
    private UUID productId;
    private UUID orderId;
    private Pageable pageable;

    private Branch activeBranch;
    private ProductVariant variant;
    private Product product;
    private Customer customer;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        branchId   = UUID.randomUUID();
        variantId  = UUID.randomUUID();
        productId  = UUID.randomUUID();
        orderId    = UUID.randomUUID();
        pageable   = PageRequest.of(0, 10);

        activeBranch = new Branch();
        activeBranch.setId(branchId);
        activeBranch.setName("Branch A");
        activeBranch.setActive(true);

        product = new Product();
        product.setId(productId);
        product.setName("Kính Ray-Ban");

        variant = new ProductVariant();
        variant.setId(variantId);
        variant.setProductId(productId);
        variant.setSku("SKU-001");
        variant.setPrice(new BigDecimal("500000"));
        variant.setStock(10);

        customer = new Customer();
        customer.setId(customerId);
        customer.setFullName("Nguyen Van A");
        customer.setEmail("a@example.com");

        savedOrder = new Order();
        savedOrder.setId(orderId);
        savedOrder.setCustomerId(customerId);
        savedOrder.setBranchId(branchId);
    }

    private OrderRequest buildPickupRequest() {
        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
        itemReq.setProductVariantId(variantId);
        itemReq.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setBranchId(branchId);
        request.setOrderType(OrderType.PICKUP);
        request.setItems(List.of(itemReq));
        return request;
    }

    @Test
    @DisplayName("create PICKUP order should set shippingFee = 0 and save")
    void create_pickup_success() {
        OrderRequest request = buildPickupRequest();

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));
        when(variantRepository.findAllById(any())).thenReturn(List.of(variant));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(orderId);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        OrderResponse result = service.create(customerId, request);

        assertThat(result.getShippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("1000000")); // 500k * 2
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result.getBranchName()).isEqualTo("Branch A");
        assertThat(result.getItems()).hasSize(1);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("create DELIVERY order should add shippingFee = 30000")
    void create_delivery_shippingFee() {
        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
        itemReq.setProductVariantId(variantId);
        itemReq.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setBranchId(branchId);
        request.setOrderType(OrderType.DELIVERY);
        request.setReceiverName("A");
        request.setReceiverPhone("0901234567");
        request.setDeliveryAddress("123 Street");
        request.setItems(List.of(itemReq));

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));
        when(variantRepository.findAllById(any())).thenReturn(List.of(variant));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(orderId);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        OrderResponse result = service.create(customerId, request);

        assertThat(result.getShippingFee()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("530000"));
    }

    @Test
    @DisplayName("create should throw BAD_REQUEST when DELIVERY missing required fields")
    void create_delivery_missingFields() {
        OrderRequest request = new OrderRequest();
        request.setBranchId(branchId);
        request.setOrderType(OrderType.DELIVERY);
        // receiverName, receiverPhone, deliveryAddress all null
        request.setItems(List.of(new OrderRequest.OrderItemRequest()));

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when branch inactive or missing")
    void create_branchNotFound() {
        OrderRequest request = buildPickupRequest();
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when branch is inactive")
    void create_branchInactive() {
        activeBranch.setActive(false);
        OrderRequest request = buildPickupRequest();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when variant not found")
    void create_variantNotFound() {
        OrderRequest request = buildPickupRequest();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));
        when(variantRepository.findAllById(any())).thenReturn(List.of()); // empty
        when(productRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should not fail order when email send fails")
    void create_emailFailDoesNotRollback() {
        OrderRequest request = buildPickupRequest();
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));
        when(variantRepository.findAllById(any())).thenReturn(List.of(variant));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(orderId);
            return o;
        });
        when(orderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doThrow(new RuntimeException("SMTP error")).when(mailService)
                .send(anyString(), anyString(), anyString(), anyMap());

        // should not throw
        OrderResponse result = service.create(customerId, request);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("findById should return order with items and branch name")
    void findById_success() {
        savedOrder.setBranchId(branchId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        OrderResponse result = service.findById(orderId);

        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getBranchName()).isEqualTo("Branch A");
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when order missing")
    void findById_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(orderId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("updateStatus should update order status and save")
    void updateStatus_success() {
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setBranchId(branchId);

        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        OrderResponse result = service.updateStatus(orderId, request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateStatus should set cancelledReason when provided")
    void updateStatus_withCancelReason() {
        savedOrder.setBranchId(branchId);
        OrderStatusRequest request = new OrderStatusRequest();
        request.setStatus(OrderStatus.CANCELLED);
        request.setCancelledReason("Customer requested");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        OrderResponse result = service.updateStatus(orderId, request);

        assertThat(result.getCancelledReason()).isEqualTo("Customer requested");
    }

    @Test
    @DisplayName("updatePaymentStatus should update payment status")
    void updatePaymentStatus_success() {
        savedOrder.setBranchId(branchId);
        savedOrder.setPaymentStatus(PaymentStatus.UNPAID);
        PaymentStatusRequest request = new PaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        OrderResponse result = service.updatePaymentStatus(orderId, request);

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("updatePaymentStatus should throw NOT_FOUND when order missing")
    void updatePaymentStatus_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        PaymentStatusRequest request = new PaymentStatusRequest();
        request.setPaymentStatus(PaymentStatus.PAID);

        assertThatThrownBy(() -> service.updatePaymentStatus(orderId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ─── findByCustomer ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByCustomer should return paginated orders for customer with branch name")
    void findByCustomer_success() {
        Page<Order> page = new PageImpl<>(List.of(savedOrder));
        when(orderRepository.findByCustomerId(customerId, pageable)).thenReturn(page);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(List.of());
        when(branchRepository.findAllById(any())).thenReturn(List.of(activeBranch));

        var result = service.findByCustomer(customerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(orderId);
        assertThat(result.getContent().get(0).getBranchName()).isEqualTo("Branch A");
    }

    @Test
    @DisplayName("findByCustomer should return empty page when customer has no orders")
    void findByCustomer_empty() {
        when(orderRepository.findByCustomerId(customerId, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.findByCustomer(customerId, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ─── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return all orders paginated with branch names")
    void findAll_success() {
        Page<Order> page = new PageImpl<>(List.of(savedOrder));
        when(orderRepository.findAll(pageable)).thenReturn(page);
        when(orderItemRepository.findByOrderIdIn(any())).thenReturn(List.of());
        when(branchRepository.findAllById(any())).thenReturn(List.of(activeBranch));

        var result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("findAll should return empty page when no orders exist")
    void findAll_empty() {
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
