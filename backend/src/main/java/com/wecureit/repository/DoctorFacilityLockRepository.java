package com.wecureit.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wecureit.entity.DoctorFacilityLock;

@Repository
public interface DoctorFacilityLockRepository extends JpaRepository<DoctorFacilityLock, UUID> {

    Optional<DoctorFacilityLock> findByDoctorIdAndWorkDate(UUID doctorId, LocalDate workDate);

    boolean existsByDoctorIdAndWorkDate(UUID doctorId, LocalDate workDate);

    void deleteByDoctorIdAndWorkDate(UUID doctorId, LocalDate workDate);
}
