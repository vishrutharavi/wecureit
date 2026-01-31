package com.wecureit.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.DoctorAvailability;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, UUID> {
    List<DoctorAvailability> findByDoctorIdAndWorkDateBetween(UUID doctorId, LocalDate from, LocalDate to);
    boolean existsByDoctorIdAndFacilityIdAndWorkDateAndStartTimeAndEndTime(UUID doctorId, UUID facilityId, LocalDate workDate, LocalTime startTime, LocalTime endTime);
}
