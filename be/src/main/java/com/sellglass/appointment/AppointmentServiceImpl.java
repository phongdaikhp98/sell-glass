package com.sellglass.appointment;

import com.sellglass.appointment.dto.AppointmentRequest;
import com.sellglass.appointment.dto.AppointmentResponse;
import com.sellglass.branch.Branch;
import com.sellglass.branch.BranchRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;

    @Override
    public PageResponse<AppointmentResponse> findByCustomer(UUID customerId, Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findByCustomerId(customerId, pageable);
        Map<UUID, String> branchNameMap = buildBranchNameMap(page);
        return PageResponse.of(page.map(a -> AppointmentResponse.from(a, branchNameMap.get(a.getBranchId()))));
    }

    @Override
    public PageResponse<AppointmentResponse> findAll(Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findAll(pageable);
        Map<UUID, String> branchNameMap = buildBranchNameMap(page);
        return PageResponse.of(page.map(a -> AppointmentResponse.from(a, branchNameMap.get(a.getBranchId()))));
    }

    @Override
    public AppointmentResponse findById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Appointment not found"));
        String branchName = branchRepository.findById(appointment.getBranchId())
                .map(Branch::getName)
                .orElse(null);
        return AppointmentResponse.from(appointment, branchName);
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
        return AppointmentResponse.from(appointmentRepository.save(appointment), branch.getName());
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
        Appointment saved = appointmentRepository.save(appointment);
        String branchName = branchRepository.findById(saved.getBranchId())
                .map(Branch::getName)
                .orElse(null);
        return AppointmentResponse.from(saved, branchName);
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

    private Map<UUID, String> buildBranchNameMap(Page<Appointment> page) {
        Set<UUID> branchIds = page.getContent().stream()
                .map(Appointment::getBranchId)
                .collect(Collectors.toSet());
        return branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));
    }
}
