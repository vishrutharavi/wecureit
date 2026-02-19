package com.wecureit.service;

import com.wecureit.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Validates whether a candidate slot can be booked.
 * Performs checks for past time, consecutive availability, and overlap with existing appointments.
 *
 * Note: Break rule enforcement is handled upstream by PatientBookingService.getAvailabilitySlots(),
 * which marks slots as BREAK_ENFORCED. Those slots are already excluded from the unbooked set
 * before reaching this service.
 */
@Service
public class SlotValidationService {

    private static final Logger logger = LoggerFactory.getLogger(SlotValidationService.class);

    /**
     * Validates a candidate slot against all rules.
     * Returns true if the slot is valid and can be considered for suggestion.
     */
    public boolean isValidCandidate(LocalDate date, LocalTime candidateStart, int durationMinutes,
                                    Set<LocalTime> unbookedSlotTimes, List<Appointment> activeAppts) {
        LocalTime slotEndTime = candidateStart.plusMinutes(durationMinutes);

        // CHECK 0: Skip slots in the past (only for today)
        if (date.equals(LocalDate.now()) && candidateStart.isBefore(LocalTime.now())) {
            logger.trace("Rejected {}: slot is in the past", candidateStart);
            return false;
        }

        // CHECK 1: The start slot must be in the unbooked set.
        // PatientBookingService.getAvailabilitySlots() already simulates the full desired duration
        // when computing each slot's status (BOOKED/UNAVAILABLE/BREAK_ENFORCED), so a slot that
        // is AVAILABLE as a start time has already passed overlap, break, and room checks for the
        // full window. Checking sub-slots here would incorrectly reject valid blocks whose later
        // sub-slots happen to be invalid as independent start times (same bug as frontend fix).
        if (!unbookedSlotTimes.contains(candidateStart)) {
            logger.trace("Rejected {}: start slot not available", candidateStart);
            return false;
        }

        // CHECK 2: Full duration must not overlap any active appointment
        LocalDateTime candidateStartDt = date.atTime(candidateStart);
        LocalDateTime candidateEndDt = date.atTime(slotEndTime);
        for (Appointment appt : activeAppts) {
            if (appt.getStartTime().isBefore(candidateEndDt) && appt.getEndTime().isAfter(candidateStartDt)) {
                logger.trace("Rejected {}: overlaps appointment {}-{}",
                        candidateStart, appt.getStartTime().toLocalTime(), appt.getEndTime().toLocalTime());
                return false;
            }
        }

        return true;
    }

    /**
     * Filters appointments to only active ones with valid start/end times.
     */
    public List<Appointment> filterActiveAppointments(List<Appointment> appointments) {
        return appointments.stream()
                .filter(appt -> appt.getIsActive() == null || appt.getIsActive())
                .filter(appt -> appt.getStartTime() != null && appt.getEndTime() != null)
                .toList();
    }
}
