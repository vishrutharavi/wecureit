package com.wecureit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wecureit.entity.Referral;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    List<Referral> findByFromDoctorIdOrderByCreatedAtDesc(UUID fromDoctorId);

    List<Referral> findByToDoctorIdOrderByCreatedAtDesc(UUID toDoctorId);

    List<Referral> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    long countByToDoctorIdAndStatus(UUID toDoctorId, String status);
}
