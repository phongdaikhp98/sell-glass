package com.sellglass.appointment.dto;

import com.sellglass.appointment.Appointment;
import com.sellglass.appointment.AppointmentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AppointmentResponse {

    private UUID id;
    private UUID customerId;
    private UUID branchId;
    private String branchName;
    private UUID staffId;
    private LocalDateTime scheduledAt;
    private AppointmentStatus status;
    private String note;
    private String resultNote;
    private LocalDateTime createdAt;

    public static AppointmentResponse from(Appointment appointment, String branchName) {
        AppointmentResponse response = new AppointmentResponse();
        response.id = appointment.getId();
        response.customerId = appointment.getCustomerId();
        response.branchId = appointment.getBranchId();
        response.branchName = branchName;
        response.staffId = appointment.getStaffId();
        response.scheduledAt = appointment.getScheduledAt();
        response.status = appointment.getStatus();
        response.note = appointment.getNote();
        response.resultNote = appointment.getResultNote();
        response.createdAt = appointment.getCreatedAt();
        return response;
    }
}
