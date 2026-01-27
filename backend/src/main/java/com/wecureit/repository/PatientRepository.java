package com.wecureit.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByFirebaseUid(String firebaseUid);
    Optional<Patient> findByEmail(String email);
}
