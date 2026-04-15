package com.sellglass.appointment;

import com.sellglass.appointment.dto.AppointmentRequest;
import com.sellglass.appointment.dto.AppointmentResponse;
import com.sellglass.branch.Branch;
import com.sellglass.branch.BranchRepository;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private AppointmentServiceImpl service;

    private UUID customerId;
    private UUID branchId;
    private UUID appointmentId;
    private Branch activeBranch;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();

        activeBranch = new Branch();
        activeBranch.setId(branchId);
        activeBranch.setName("Branch 1");
        activeBranch.setActive(true);
        activeBranch.setOpenTime(LocalTime.of(8, 0));
        activeBranch.setCloseTime(LocalTime.of(20, 0));

        appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setCustomerId(customerId);
        appointment.setBranchId(branchId);
        appointment.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        appointment.setStatus(AppointmentStatus.PENDING);
    }

    @Test
    @DisplayName("findByCustomer should return page with branch name enriched")
    void findByCustomer_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> page = new PageImpl<>(List.of(appointment));
        when(appointmentRepository.findByCustomerId(customerId, pageable)).thenReturn(page);
        when(branchRepository.findAllById(any())).thenReturn(List.of(activeBranch));

        PageResponse<AppointmentResponse> result = service.findByCustomer(customerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBranchName()).isEqualTo("Branch 1");
        verify(appointmentRepository).findByCustomerId(customerId, pageable);
    }

    @Test
    @DisplayName("findAll should return page with branch name enriched")
    void findAll_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Appointment> page = new PageImpl<>(List.of(appointment));
        when(appointmentRepository.findAll(pageable)).thenReturn(page);
        when(branchRepository.findAllById(any())).thenReturn(List.of(activeBranch));

        PageResponse<AppointmentResponse> result = service.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBranchName()).isEqualTo("Branch 1");
    }

    @Test
    @DisplayName("findById should return appointment with branch name")
    void findById_success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        AppointmentResponse result = service.findById(appointmentId);

        assertThat(result.getId()).isEqualTo(appointmentId);
        assertThat(result.getBranchName()).isEqualTo("Branch 1");
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when appointment missing")
    void findById_notFound() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(appointmentId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save appointment within branch hours")
    void create_success() {
        AppointmentRequest request = new AppointmentRequest();
        request.setBranchId(branchId);
        request.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        request.setNote("Test note");

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse result = service.create(customerId, request);

        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getBranchId()).isEqualTo(branchId);
        assertThat(result.getNote()).isEqualTo("Test note");
        assertThat(result.getBranchName()).isEqualTo("Branch 1");
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when branch not found")
    void create_branchNotFound() {
        AppointmentRequest request = new AppointmentRequest();
        request.setBranchId(branchId);
        request.setScheduledAt(LocalDateTime.now().plusDays(1));

        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when branch inactive")
    void create_branchInactive() {
        activeBranch.setActive(false);
        AppointmentRequest request = new AppointmentRequest();
        request.setBranchId(branchId);
        request.setScheduledAt(LocalDateTime.now().plusDays(1));

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw BAD_REQUEST when time before branch open")
    void create_timeBeforeOpen() {
        AppointmentRequest request = new AppointmentRequest();
        request.setBranchId(branchId);
        request.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(7).withMinute(0));

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw BAD_REQUEST when time after branch close")
    void create_timeAfterClose() {
        AppointmentRequest request = new AppointmentRequest();
        request.setBranchId(branchId);
        request.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(21).withMinute(0));

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        assertThatThrownBy(() -> service.create(customerId, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("updateStatus should update status and resultNote")
    void updateStatus_success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        AppointmentResponse result = service.updateStatus(appointmentId, AppointmentStatus.DONE, "All good");

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.DONE);
        assertThat(result.getResultNote()).isEqualTo("All good");
    }

    @Test
    @DisplayName("updateStatus should not overwrite resultNote when null passed")
    void updateStatus_noteNull() {
        appointment.setResultNote("existing note");
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(activeBranch));

        AppointmentResponse result = service.updateStatus(appointmentId, AppointmentStatus.CONFIRMED, null);

        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(result.getResultNote()).isEqualTo("existing note");
    }

    @Test
    @DisplayName("cancel should set status to CANCELLED for owner")
    void cancel_success() {
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        service.cancel(appointmentId, customerId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("cancel should throw FORBIDDEN when not owner")
    void cancel_notOwner() {
        UUID otherCustomer = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(appointmentId, otherCustomer))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancel should throw BAD_REQUEST when appointment already DONE")
    void cancel_alreadyDone() {
        appointment.setStatus(AppointmentStatus.DONE);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> service.cancel(appointmentId, customerId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(appointmentRepository, never()).save(any());
    }
}
