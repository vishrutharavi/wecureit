package com.wecureit.model;

import com.wecureit.entity.Appointment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a search node for the branch and bound algorithm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotSearchNode {
    private LocalDate date;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private int durationMinutes;
    private double actualScore;
    private double boundScore;
    private List<Appointment> existingAppts;
    private List<ScheduleGap> gaps;
    private String availabilityId;
    private UUID facilityId;
    private String facilityName;
    private UUID doctorId;
    private String doctorName;
    private String specialty;
    private String specialtyId;
}
