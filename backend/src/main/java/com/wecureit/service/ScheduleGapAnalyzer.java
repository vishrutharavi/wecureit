package com.wecureit.service;

import com.wecureit.entity.Appointment;
import com.wecureit.model.ScheduleGap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Detects gaps (idle time) in a doctor's schedule for a given date.
 */
@Service
public class ScheduleGapAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleGapAnalyzer.class);

    /**
     * Detect gaps in a doctor's schedule, including gaps before the first
     * appointment and after the last appointment within the office hours window.
     */
    public List<ScheduleGap> detectGaps(LocalDate date, List<Appointment> appointments,
                                        LocalTime officeStart, LocalTime officeEnd) {
        List<ScheduleGap> gaps = new ArrayList<>();

        List<Appointment> active = filterActiveAppointments(appointments);

        if (active.isEmpty()) {
            long totalMinutes = Duration.between(officeStart, officeEnd).toMinutes();
            if (totalMinutes > 0) {
                gaps.add(new ScheduleGap(officeStart, officeEnd, totalMinutes));
            }
            logger.debug("detectGaps date={} - no active appointments, entire window is gap: {}-{} ({}min)",
                    date, officeStart, officeEnd, totalMinutes);
            return gaps;
        }

        List<Appointment> sorted = new ArrayList<>(active);
        sorted.sort(Comparator.comparing(Appointment::getStartTime));

        // Gap before first appointment
        LocalTime firstApptStart = sorted.get(0).getStartTime().toLocalTime();
        if (officeStart.isBefore(firstApptStart)) {
            long gapMinutes = Duration.between(officeStart, firstApptStart).toMinutes();
            if (gapMinutes > 0) {
                gaps.add(new ScheduleGap(officeStart, firstApptStart, gapMinutes));
            }
        }

        // Gaps between consecutive appointments
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDateTime currentEnd = sorted.get(i).getEndTime();
            LocalDateTime nextStart = sorted.get(i + 1).getStartTime();

            if (currentEnd.isBefore(nextStart)) {
                long gapMinutes = Duration.between(currentEnd, nextStart).toMinutes();
                if (gapMinutes > 0) {
                    gaps.add(new ScheduleGap(
                            currentEnd.toLocalTime(),
                            nextStart.toLocalTime(),
                            gapMinutes
                    ));
                }
            }
        }

        // Gap after last appointment
        LocalTime lastApptEnd = sorted.get(sorted.size() - 1).getEndTime().toLocalTime();
        if (lastApptEnd.isBefore(officeEnd)) {
            long gapMinutes = Duration.between(lastApptEnd, officeEnd).toMinutes();
            if (gapMinutes > 0) {
                gaps.add(new ScheduleGap(lastApptEnd, officeEnd, gapMinutes));
            }
        }

        logger.debug("detectGaps date={} - activeAppts={}, gapsFound={}", date, active.size(), gaps.size());
        return gaps;
    }

    private List<Appointment> filterActiveAppointments(List<Appointment> appointments) {
        List<Appointment> active = new ArrayList<>();
        for (Appointment appt : appointments) {
            if (appt.getIsActive() != null && !appt.getIsActive()) continue;
            if (appt.getStartTime() == null || appt.getEndTime() == null) continue;
            active.add(appt);
        }
        return active;
    }
}
