package com.sellglass.appointment;

import com.sellglass.appointment.dto.AppointmentRequest;
import com.sellglass.appointment.dto.AppointmentResponse;
import com.sellglass.branch.Branch;
import com.sellglass.branch.BranchRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;

    @Override
    public PageResponse<AppointmentResponse> findByCustomer(UUID customerId, Pageable pageable) {
        return PageResponse.of(appointmentRepository.findByCustomerId(customerId, pageable)
                .map(AppointmentResponse::from));
    }

    @Override
    public PageResponse<AppointmentResponse> findAll(Pageable pageable) {
        return PageResponse.of(appointmentRepository.findAll(pageable).map(AppointmentResponse::from));
    }

    @Override
    public AppointmentResponse findById(UUID id) {
        return AppointmentResponse.from(appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Appointment not found")));
    }

    @Override
    @Transactional
    public AppointmentResponse create(UUID customerId, AppointmentRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .filter(Branch::isActive)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Branch not found or inactive"));

        if (request.getScheduledAt() != null && branch.getOpenTime() != null && branch.getCloseTime() != null) {
            var time = request.getScheduledAt().toLocalTime();
            if (time.isBefore(branch.getOpenTime()) || time.isAfter(branch.getCloseTime())) {
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Scheduled time is outside branch hours (" + branch.getOpenTime() + " - " + branch.getCloseTime() + ")");
            }
        }

        Appointment appointment = new Appointment();
        appointment.setCustomerId(customerId);
        appointment.setBranchId(request.getBranchId());
        appointment.setScheduledAt(request.getScheduledAt());
        appointment.setNote(request.getNote());
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(UUID id, AppointmentStatus status, String resultNote) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Appointment not found"));
        appointment.setStatus(status);
        if (resultNote != null) {
            appointment.setResultNote(resultNote);
        }
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public void cancel(UUID id, UUID customerId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Appointment not found"));
        if (!appointment.getCustomerId().equals(customerId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "Appointment does not belong to this customer");
        }
        if (appointment.getStatus() == AppointmentStatus.DONE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Cannot cancel a completed appointment");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }
}
