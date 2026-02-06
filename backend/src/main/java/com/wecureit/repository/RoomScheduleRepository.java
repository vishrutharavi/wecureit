package com.wecureit.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wecureit.entity.RoomSchedule;

public interface RoomScheduleRepository extends JpaRepository<RoomSchedule, UUID> {

    @Query("SELECT rs FROM RoomSchedule rs WHERE rs.roomId = :roomId AND ((rs.startAt < :endAt) AND (rs.endAt > :startAt))")
    List<RoomSchedule> findOverlapping(@Param("roomId") UUID roomId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("SELECT rs FROM RoomSchedule rs WHERE rs.doctorId = :doctorId AND ((rs.startAt < :endAt) AND (rs.endAt > :startAt)) AND rs.appointmentId IS NOT NULL")
    List<RoomSchedule> findAppointmentsForDoctor(@Param("doctorId") UUID doctorId, @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);

    @Query("SELECT rs FROM RoomSchedule rs WHERE rs.appointmentId = :appointmentId")
    List<RoomSchedule> findByAppointmentId(@Param("appointmentId") UUID appointmentId);

}
