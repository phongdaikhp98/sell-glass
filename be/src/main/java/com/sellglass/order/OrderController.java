package com.sellglass.order;

import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.ApiResponse;
import com.sellglass.common.response.PageResponse;
import com.sellglass.order.dto.OrderRequest;
import com.sellglass.order.dto.OrderResponse;
import com.sellglass.order.dto.OrderStatusRequest;
import com.sellglass.order.dto.PaymentStatusRequest;
import com.sellglass.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Customer endpoints
    @GetMapping("/v1/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(orderService.findByCustomer(userDetails.getUserId(), pageable)));
    }

    @GetMapping("/v1/orders/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        OrderResponse order = orderService.findById(id);
        if (!order.getCustomerId().equals(userDetails.getUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Order does not belong to this customer");
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/v1/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.create(userDetails.getUserId(), request)));
    }

    // Admin/Staff endpoints
    @GetMapping("/v1/admin/orders")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(orderService.findAll(pageable)));
    }

    @GetMapping("/v1/admin/orders/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.findById(id)));
    }

    @PatchMapping("/v1/admin/orders/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateStatus(id, request)));
    }

    @PatchMapping("/v1/admin/orders/{id}/payment-status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updatePaymentStatus(id, request)));
    }
}
