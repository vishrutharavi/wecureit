package com.wecureit.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.Speciality;


public interface SpecialityRepository extends JpaRepository<Speciality, String> {
    boolean existsBySpecialityNameIgnoreCase(String name);
    Optional<Speciality> findBySpecialityNameIgnoreCase(String name);
}

