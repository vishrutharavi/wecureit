package com.wecureit.dto.request;

import java.util.UUID;

public class CreateReferralRequest {

    private UUID patientId;
    private UUID toDoctorId;
    private Long appointmentId;
    private String specialityCode;
    private String reason;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public UUID getToDoctorId() { return toDoctorId; }
    public void setToDoctorId(UUID toDoctorId) { this.toDoctorId = toDoctorId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
