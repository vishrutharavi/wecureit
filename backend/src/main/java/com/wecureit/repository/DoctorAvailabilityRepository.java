package com.wecureit.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.entity.DoctorAvailability;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, UUID> {

    List<DoctorAvailability> findByDoctorIdAndWorkDateBetween(UUID doctorId, LocalDate from, LocalDate to);

    List<DoctorAvailability> findByDoctorIdAndFacilityIdAndWorkDate(UUID doctorId, UUID facilityId, LocalDate workDate);

    boolean existsByDoctorIdAndFacilityIdAndWorkDateAndStartTimeAndEndTime(UUID doctorId, UUID facilityId,
            LocalDate workDate, LocalTime startTime, LocalTime endTime);

    @Modifying
    @Transactional
    @Query("UPDATE DoctorAvailability d SET d.isActive = false WHERE d.doctorId = :doctorId AND d.workDate = :workDate AND d.facilityId <> :facilityId")
    int deactivateOtherAvailabilities(@Param("doctorId") UUID doctorId, @Param("workDate") LocalDate workDate,
            @Param("facilityId") UUID facilityId);

}
