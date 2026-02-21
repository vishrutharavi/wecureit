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
    Optional<Appointment> findByUuid(UUID uuid);
   
    @Query("SELECT DISTINCT a FROM Appointment a WHERE a.doctorAvailability.doctorId = :doctorId AND a.isActive = true AND ((a.startTime < :endAt) AND (a.endTime > :startAt))")
    List<Appointment> findAppointmentsForDoctor(@Param("doctorId") UUID doctorId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("SELECT DISTINCT a FROM Appointment a WHERE a.doctorAvailability.doctorId = :doctorId AND ((a.startTime < :endAt) AND (a.endTime > :startAt))")
    List<Appointment> findAllAppointmentsForDoctor(@Param("doctorId") UUID doctorId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("SELECT a FROM Appointment a WHERE a.facility.id = :facilityId AND ((a.startTime < :endAt) AND (a.endTime > :startAt))")
    List<Appointment> findAppointmentsForFacility(@Param("facilityId") UUID facilityId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("SELECT a FROM Appointment a WHERE a.room.id = :roomId AND a.isActive = true AND a.startTime < :endAt AND a.endTime > :startAt")
    List<Appointment> findActiveAppointmentsByRoomAndTimeOverlap(@Param("roomId") UUID roomId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("SELECT a FROM Appointment a WHERE a.facility.id = :facilityId AND a.room IS NOT NULL AND a.isActive = true AND a.startTime < :endAt AND a.endTime > :startAt")
    List<Appointment> findActiveAppointmentsWithRoomForFacility(@Param("facilityId") UUID facilityId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}
