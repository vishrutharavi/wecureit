package com.wecureit.repository;

import com.wecureit.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByFirebaseUid(String firebaseUid);
    Optional<Patient> findByEmail(String email);
}
