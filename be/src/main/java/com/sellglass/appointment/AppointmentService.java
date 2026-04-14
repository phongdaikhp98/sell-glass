package com.sellglass.appointment;

import com.sellglass.appointment.dto.AppointmentRequest;
import com.sellglass.appointment.dto.AppointmentResponse;
import com.sellglass.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AppointmentService {

    PageResponse<AppointmentResponse> findByCustomer(UUID customerId, Pageable pageable);

    PageResponse<AppointmentResponse> findAll(Pageable pageable);

    AppointmentResponse findById(UUID id);

    AppointmentResponse create(UUID customerId, AppointmentRequest request);

    AppointmentResponse updateStatus(UUID id, AppointmentStatus status, String resultNote);

    void cancel(UUID id, UUID customerId);
}
