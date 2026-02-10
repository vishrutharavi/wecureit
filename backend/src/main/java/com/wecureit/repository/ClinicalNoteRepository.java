package com.wecureit.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.ClinicalNote;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {
    java.util.List<ClinicalNote> findByAppointmentHistoryId(java.util.UUID appointmentHistoryId);
    java.util.List<ClinicalNote> findByAppointmentId(java.util.UUID appointmentId);
    java.util.List<ClinicalNote> findByPatientId(java.util.UUID patientId);
    java.util.List<ClinicalNote> findByDoctorId(java.util.UUID doctorId);
}
