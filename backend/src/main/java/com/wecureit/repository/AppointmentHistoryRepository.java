package com.wecureit.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.AppointmentHistory;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {
	java.util.List<AppointmentHistory> findByPatientId(java.util.UUID patientId);
    java.util.List<AppointmentHistory> findByAppointmentId(java.util.UUID appointmentId);
	java.util.List<AppointmentHistory> findByStatusIgnoreCase(String status);

}
