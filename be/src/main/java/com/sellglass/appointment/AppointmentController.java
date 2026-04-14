package com.sellglass.appointment;

import com.sellglass.appointment.dto.AppointmentRequest;
import com.sellglass.appointment.dto.AppointmentResponse;
import com.sellglass.common.response.ApiResponse;
import com.sellglass.common.response.PageResponse;
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
public class AppointmentController {

    private final AppointmentService appointmentService;

    // Customer endpoints
    @GetMapping("/v1/appointments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getMyAppointments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("scheduledAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                appointmentService.findByCustomer(userDetails.getUserId(), pageable)));
    }

    @PostMapping("/v1/appointments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(appointmentService.create(userDetails.getUserId(), request)));
    }

    @PatchMapping("/v1/appointments/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID id) {
        appointmentService.cancel(id, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Admin/Staff endpoints
    @GetMapping("/v1/admin/appointments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<AppointmentResponse>>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("scheduledAt").ascending());
        return ResponseEntity.ok(ApiResponse.success(appointmentService.findAll(pageable)));
    }

    @GetMapping("/v1/admin/appointments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.findById(id)));
    }

    @PatchMapping("/v1/admin/appointments/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BRANCH_MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam AppointmentStatus status,
            @RequestParam(required = false) String resultNote) {
        return ResponseEntity.ok(ApiResponse.success(appointmentService.updateStatus(id, status, resultNote)));
    }
}
