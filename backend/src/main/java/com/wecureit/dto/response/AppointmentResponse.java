package com.wecureit.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private LocalDate date;
    private Long duration;
    private UUID patientId;
    private String patientName;
    private UUID doctorAvailabilityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UUID facilityId;
    private String specialityId;
    private String facilityName;
    private String specialityName;
    private String doctorName;
    private Boolean isActive;
    private String chiefComplaints;
    // actor who cancelled the appointment, e.g. 'patient' or 'doctor' (normalized lowercase)
    private String cancelledBy;
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
