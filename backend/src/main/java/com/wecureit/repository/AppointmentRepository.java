package com.wecureit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wecureit.entity.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByStartTimeDesc(UUID patientId);

    boolean existsByPatientIdAndStartTime(UUID patientId, LocalDateTime startTime);
    boolean existsByPatientIdAndStartTimeAndIsActiveTrue(UUID patientId, LocalDateTime startTime);

    Optional<Appointment> findByPatientIdAndStartTime(UUID patientId, LocalDateTime startTime);
   
    @Query("SELECT a FROM Appointment a WHERE a.doctorAvailability.doctorId = :doctorId AND ((a.startTime < :endAt) AND (a.endTime > :startAt))")
    List<Appointment> findAppointmentsForDoctor(@Param("doctorId") UUID doctorId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}
