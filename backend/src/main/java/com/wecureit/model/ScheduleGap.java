package com.wecureit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Represents a gap (idle time) in a doctor's schedule.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGap {
    private LocalTime start;
    private LocalTime end;
    private long durationMinutes;
}
