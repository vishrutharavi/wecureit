package com.wecureit.dto.response;

import java.util.UUID;

public class DoctorLicenseResponse {

    private UUID id;
    private UUID doctorId;
    private String stateCode;
    private String specialityCode;
    private boolean isActive;
    private String stateName;
    private String specialityName;

    public DoctorLicenseResponse(UUID id, UUID doctorId, String stateCode, String specialityCode, boolean isActive, String stateName, String specialityName) {
        this.id = id;
        this.doctorId = doctorId;
        this.stateCode = stateCode;
        this.specialityCode = specialityCode;
        this.isActive = isActive;
        this.stateName = stateName;
        this.specialityName = specialityName;
    }

    public UUID getId() { return id; }
    public UUID getDoctorId() { return doctorId; }
    public String getStateCode() { return stateCode; }
    public String getSpecialityCode() { return specialityCode; }
    public boolean isActive() { return isActive; }
    public String getStateName() { return stateName; }
    public String getSpecialityName() { return specialityName; }
}
