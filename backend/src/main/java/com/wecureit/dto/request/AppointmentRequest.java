package com.wecureit.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AppointmentRequest {
    private LocalDate date;
    private Long duration;
    private UUID patientId;
    private UUID doctorAvailabilityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UUID facilityId;
    private String specialityId;
    private Boolean isActive;
    private String chiefComplaints;
}
