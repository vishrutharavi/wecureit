package com.wecureit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.DoctorLicense;

public interface DoctorLicenseRepository extends JpaRepository<DoctorLicense, UUID> {

    List<DoctorLicense> findByDoctorIdAndIsActiveTrue(UUID doctorId);

    List<DoctorLicense> findByStateCodeAndSpecialityCodeAndIsActiveTrue(
        String stateCode,
        String specialityCode
    );
}

