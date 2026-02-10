package com.wecureit.dto.request;

public class AvailabilityRequest {
    // accept workDate as a plain local-date string (YYYY-MM-DD) or an ISO datetime string; service will parse to LocalDate
    private String workDate;
    private String startTime; // HH:mm
    private String endTime;   // HH:mm
    private String specialityCode;
    private String facilityId; // UUID as string

    public String getWorkDate() { return workDate; }
    public void setWorkDate(String workDate) { this.workDate = workDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }
}
