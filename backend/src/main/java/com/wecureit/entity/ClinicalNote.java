package com.wecureit.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clinical_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ClinicalNote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "appointment_history_id", columnDefinition = "uuid")
    private UUID appointmentHistoryId;

    @Column(name = "appointment_id", columnDefinition = "uuid")
    private UUID appointmentId;

    @Column(name = "patient_id", columnDefinition = "uuid")
    private UUID patientId;

    @Column(name = "doctor_id", columnDefinition = "uuid")
    private UUID doctorId;

    @Column(name = "note_text", columnDefinition = "text")
    private String noteText;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

}
