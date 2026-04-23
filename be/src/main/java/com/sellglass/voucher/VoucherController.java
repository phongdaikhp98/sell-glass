package com.sellglass.voucher;

import com.sellglass.common.response.ApiResponse;
import com.sellglass.common.response.PageResponse;
import com.sellglass.voucher.dto.ApplyVoucherRequest;
import com.sellglass.voucher.dto.ApplyVoucherResponse;
import com.sellglass.voucher.dto.VoucherRequest;
import com.sellglass.voucher.dto.VoucherResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // ─── Public ───────────────────────────────────────────────────────────────

    @PostMapping("/v1/vouchers/apply")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ApplyVoucherResponse>> apply(
            @Valid @RequestBody ApplyVoucherRequest request) {
        ApplyVoucherResponse result = voucherService.apply(request.getCode(), request.getOrderTotal());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ─── Admin ────────────────────────────────────────────────────────────────

    @GetMapping("/v1/admin/vouchers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<VoucherResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PageResponse<VoucherResponse> result = voucherService.findAll(search, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/v1/admin/vouchers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> create(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(voucherService.create(request)));
    }

    @PutMapping("/v1/admin/vouchers/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.update(id, request)));
    }

    @DeleteMapping("/v1/admin/vouchers/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        voucherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
