package com.wecureit.dto.request;

import java.time.LocalDate;

public class AvailabilityRequest {
    private LocalDate workDate;
    private String startTime; // HH:mm
    private String endTime;   // HH:mm
    private String specialityCode;
    private String facilityId; // UUID as string

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }
}
