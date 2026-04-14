package com.sellglass.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Page<Appointment> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Appointment> findByBranchId(UUID branchId, Pageable pageable);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);
}
