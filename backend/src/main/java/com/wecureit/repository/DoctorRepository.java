package com.wecureit.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByFirebaseUid(String firebaseUid);
    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByEmailIgnoreCase(String email);
    List<Doctor> findByIsActiveTrue();

    List<Doctor> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
}
