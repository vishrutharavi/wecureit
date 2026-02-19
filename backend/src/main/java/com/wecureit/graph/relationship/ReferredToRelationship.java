package com.wecureit.graph.relationship;

import java.time.LocalDateTime;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import com.wecureit.graph.node.DoctorNode;

@RelationshipProperties
public class ReferredToRelationship {

    @RelationshipId
    @GeneratedValue
    private Long id;

    @TargetNode
    private DoctorNode toDoctor;

    private String referralId;

    private String patientPgId;

    private String specialityCode;

    private String status;

    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ReferredToRelationship() {}

    public ReferredToRelationship(DoctorNode toDoctor, String referralId, String patientPgId,
                                   String specialityCode, String status, String reason,
                                   LocalDateTime createdAt) {
        this.toDoctor = toDoctor;
        this.referralId = referralId;
        this.patientPgId = patientPgId;
        this.specialityCode = specialityCode;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DoctorNode getToDoctor() { return toDoctor; }
    public void setToDoctor(DoctorNode toDoctor) { this.toDoctor = toDoctor; }

    public String getReferralId() { return referralId; }
    public void setReferralId(String referralId) { this.referralId = referralId; }

    public String getPatientPgId() { return patientPgId; }
    public void setPatientPgId(String patientPgId) { this.patientPgId = patientPgId; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
