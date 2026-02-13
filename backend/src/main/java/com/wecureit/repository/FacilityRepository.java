package com.wecureit.repository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wecureit.entity.Facility;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    Page<Facility> findAllByIsActive(Boolean isActive, Pageable pageable);
    List<Facility> findAllByIsActive(Boolean isActive);

    @Query(value = "SELECT state_code FROM facilities WHERE id = :facilityId", nativeQuery = true)
    Optional<String> findStateCodeById(@Param("facilityId") UUID facilityId);
}
