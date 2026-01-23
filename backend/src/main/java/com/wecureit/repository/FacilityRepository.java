package com.wecureit.repository;

import java.util.UUID;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wecureit.entity.Facility;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    Page<Facility> findAllByIsActive(Boolean isActive, Pageable pageable);
    List<Facility> findAllByIsActive(Boolean isActive);
}
