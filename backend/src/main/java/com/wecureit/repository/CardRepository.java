package com.wecureit.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.wecureit.dto.response.CardResponse;
import com.wecureit.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {

    // Return last4 strings (legacy). Prefer using findCardResponsesByPatientIdOrderByIdAsc
    @Query("SELECT c.last4 FROM Card c WHERE c.patient.id = :patientId ORDER BY c.id ASC")
    List<String> findByPatientIdOrderByIdAsc(UUID patientId);

    // Return id + last4 for each card (used by API to enable delete/edit operations)
    @Query("SELECT new com.wecureit.dto.response.CardResponse(c.id, c.last4) FROM Card c WHERE c.patient.id = :patientId AND c.isActive = true ORDER BY c.id ASC")
    List<CardResponse> findCardResponsesByPatientIdOrderByIdAsc(UUID patientId);

    // Fetch full Card entities for a given patient (used for duplicate checks)
    List<Card> findAllByPatientIdOrderByIdAsc(UUID patientId);

    // return only active cards for duplicate checks and listing
    List<Card> findAllByPatientIdAndIsActiveTrueOrderByIdAsc(UUID patientId);

}
