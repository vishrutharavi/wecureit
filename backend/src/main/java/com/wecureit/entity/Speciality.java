package com.wecureit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialities")
public class Speciality {

    @Id
    @Column(name = "speciality_code", length = 4)
    private String specialityCode;

    @Column(name = "speciality_name", nullable = false, unique = true)
    private String specialityName;

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) {
        this.specialityCode = specialityCode;
    }

    public String getSpecialityName() { return specialityName; }
    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }
}
