package com.wecureit.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReferralResponse {

    private UUID id;
    private UUID patientId;
    private String patientName;
    private UUID fromDoctorId;
    private String fromDoctorName;
    private String fromDoctorEmail;
    private UUID toDoctorId;
    private String toDoctorName;
    private String toDoctorEmail;
    private String specialityCode;
    private String specialityName;
    private Long appointmentId;
    private String reason;
    private String status;
    private String cancelReason;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public UUID getFromDoctorId() { return fromDoctorId; }
    public void setFromDoctorId(UUID fromDoctorId) { this.fromDoctorId = fromDoctorId; }

    public String getFromDoctorName() { return fromDoctorName; }
    public void setFromDoctorName(String fromDoctorName) { this.fromDoctorName = fromDoctorName; }

    public String getFromDoctorEmail() { return fromDoctorEmail; }
    public void setFromDoctorEmail(String fromDoctorEmail) { this.fromDoctorEmail = fromDoctorEmail; }

    public UUID getToDoctorId() { return toDoctorId; }
    public void setToDoctorId(UUID toDoctorId) { this.toDoctorId = toDoctorId; }

    public String getToDoctorName() { return toDoctorName; }
    public void setToDoctorName(String toDoctorName) { this.toDoctorName = toDoctorName; }

    public String getToDoctorEmail() { return toDoctorEmail; }
    public void setToDoctorEmail(String toDoctorEmail) { this.toDoctorEmail = toDoctorEmail; }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public String getSpecialityName() { return specialityName; }
    public void setSpecialityName(String specialityName) { this.specialityName = specialityName; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
