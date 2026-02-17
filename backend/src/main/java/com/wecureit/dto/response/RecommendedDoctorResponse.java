package com.wecureit.dto.response;

import java.util.UUID;

public class RecommendedDoctorResponse {

    private UUID doctorId;
    private String doctorName;
    private String doctorEmail;
    private String stateName;
    private String stateCode;
    private String nextAvailableSlot;
    private int appointmentLoad;
    private String reason;

    public UUID getDoctorId() { return doctorId; }
    public void setDoctorId(UUID doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    public String getStateName() { return stateName; }
    public void setStateName(String stateName) { this.stateName = stateName; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getNextAvailableSlot() { return nextAvailableSlot; }
    public void setNextAvailableSlot(String nextAvailableSlot) { this.nextAvailableSlot = nextAvailableSlot; }

    public int getAppointmentLoad() { return appointmentLoad; }
    public void setAppointmentLoad(int appointmentLoad) { this.appointmentLoad = appointmentLoad; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
