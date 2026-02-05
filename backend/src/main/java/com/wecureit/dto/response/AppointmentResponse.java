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
    private UUID doctorAvailabilityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UUID facilityId;
    private String specialityId;
    private UUID roomScheduleId;
    private String roomNumber;
    private Boolean isActive;
    private String chiefComplaints;
}
