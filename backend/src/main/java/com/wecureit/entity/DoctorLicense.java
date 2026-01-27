package com.wecureit.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "doctor_license",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"doctor_id", "state_code", "speciality_code"})
    }
)
public class DoctorLicense {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "state_code", length = 2, nullable = false)
    private String stateCode;

    @Column(name = "speciality_code", length = 4, nullable = false)
    private String specialityCode;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public UUID getId() { return id; }

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
