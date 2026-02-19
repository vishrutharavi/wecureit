package com.wecureit.service;

import com.wecureit.entity.Appointment;
import com.wecureit.model.ScheduleGap;
import com.wecureit.model.SlotSearchNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes scores for candidate slots and builds human-readable reasons.
 * Scoring considers gap-filling efficiency, fragmentation impact, and temporal preference.
 */
@Service
public class SlotScoringService {

    private static final Logger logger = LoggerFactory.getLogger(SlotScoringService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Compute upper bound score for a node (optimistic estimate for branch and bound pruning).
     */
    public double computeBound(SlotSearchNode node) {
        double maxGapFill = 100.0;
        double minFragmentation = 0.0;
        double temporalScore = computeTemporalPreference(node.getDate(), node.getSlotStartTime());
        return 0.5 * maxGapFill + 0.3 * minFragmentation + 0.2 * temporalScore;
    }

    /**
     * Compute actual score for a slot.
     */
    public double computeSlotScore(SlotSearchNode node) {
        double gapFillScore = computeGapFillScore(node.getSlotStartTime(), node.getSlotEndTime(),
                node.getDurationMinutes(), node.getGaps(), node.getExistingAppts());

        double fragmentationPenalty = computeFragmentationPenalty(node.getSlotStartTime(), node.getSlotEndTime(),
                node.getDurationMinutes(), node.getGaps());

        double temporalScore = computeTemporalPreference(node.getDate(), node.getSlotStartTime());

        double total = 0.5 * gapFillScore + 0.3 * fragmentationPenalty + 0.2 * temporalScore;

        logger.trace("Score for {} {}: gapFill={}, frag={}, temporal={}, total={}",
                node.getDate(), node.getSlotStartTime(), gapFillScore, fragmentationPenalty, temporalScore, total);

        return total;
    }

    /**
     * Build human-readable reason for slot suggestion.
     */
    public String buildReason(SlotSearchNode node) {
        double score = node.getActualScore();
        LocalTime slotStart = node.getSlotStartTime();
        LocalTime slotEnd = node.getSlotEndTime();
        List<ScheduleGap> gaps = node.getGaps();
        List<Appointment> appts = node.getExistingAppts();

        for (ScheduleGap gap : gaps) {
            if (slotStart.equals(gap.getStart()) && slotEnd.equals(gap.getEnd())) {
                return String.format("Perfectly fills %d-min gap at %s (Score: %.1f)",
                        gap.getDurationMinutes(), slotStart.format(TIME_FORMAT), score);
            }

            if (slotStart.equals(gap.getStart()) || slotEnd.equals(gap.getEnd())) {
                return String.format("Fills gap cleanly at %s (Score: %.1f)",
                        slotStart.format(TIME_FORMAT), score);
            }

            if (!slotStart.isBefore(gap.getStart()) && !slotEnd.isAfter(gap.getEnd())) {
                return String.format("Fills %d-min gap between appointments (Score: %.1f)",
                        gap.getDurationMinutes(), score);
            }
        }

        if (isAdjacentToAppointment(slotStart, slotEnd, appts)) {
            return String.format("Adjacent to existing appointment at %s (Score: %.1f)",
                    slotStart.format(TIME_FORMAT), score);
        }

        if (appts.isEmpty()) {
            return String.format("Earliest available slot at %s (Score: %.1f)",
                    slotStart.format(TIME_FORMAT), score);
        }

        return String.format("Optimal slot at %s (Score: %.1f)",
                slotStart.format(TIME_FORMAT), score);
    }

    // ---- Private scoring methods ----

    private double computeGapFillScore(LocalTime slotStart, LocalTime slotEnd, int duration,
                                       List<ScheduleGap> gaps, List<Appointment> existingAppts) {
        for (ScheduleGap gap : gaps) {
            if (slotStart.equals(gap.getStart()) && slotEnd.equals(gap.getEnd())) {
                return 100.0;
            }
            if (slotStart.equals(gap.getStart()) || slotEnd.equals(gap.getEnd())) {
                return 80.0;
            }
            if (!slotStart.isBefore(gap.getStart()) && !slotEnd.isAfter(gap.getEnd())) {
                long remainingGap = gap.getDurationMinutes() - duration;
                if (remainingGap >= 60) return 70.0;
                if (remainingGap >= 30) return 60.0;
                if (remainingGap >= 15) return 40.0;
                return 20.0;
            }
        }

        if (isAdjacentToAppointment(slotStart, slotEnd, existingAppts)) {
            return 30.0;
        }

        if (existingAppts.isEmpty()) {
            return 50.0;
        }

        return 10.0;
    }

    private boolean isAdjacentToAppointment(LocalTime slotStart, LocalTime slotEnd,
                                            List<Appointment> existingAppts) {
        for (Appointment appt : existingAppts) {
            if (appt.getStartTime() == null || appt.getEndTime() == null) continue;
            LocalTime apptStart = appt.getStartTime().toLocalTime();
            LocalTime apptEnd = appt.getEndTime().toLocalTime();
            if (slotEnd.equals(apptStart) || apptEnd.equals(slotStart)) {
                return true;
            }
        }
        return false;
    }

    private double computeFragmentationPenalty(LocalTime slotStart, LocalTime slotEnd, int duration,
                                               List<ScheduleGap> existingGaps) {
        List<ScheduleGap> simulatedGaps = simulateSlotInsertion(slotStart, slotEnd, existingGaps);

        double penalty = 0.0;
        for (ScheduleGap gap : simulatedGaps) {
            if (gap.getDurationMinutes() < 15) {
                penalty -= 50.0;
            } else if (gap.getDurationMinutes() < 30) {
                penalty -= 20.0;
            } else if (gap.getDurationMinutes() < 60) {
                penalty -= 10.0;
            }
        }
        return penalty;
    }

    private List<ScheduleGap> simulateSlotInsertion(LocalTime slotStart, LocalTime slotEnd,
                                                     List<ScheduleGap> existingGaps) {
        List<ScheduleGap> newGaps = new ArrayList<>();

        for (ScheduleGap gap : existingGaps) {
            if (!slotStart.isBefore(gap.getStart()) && !slotEnd.isAfter(gap.getEnd())) {
                if (slotStart.isAfter(gap.getStart())) {
                    long beforeDuration = Duration.between(gap.getStart(), slotStart).toMinutes();
                    if (beforeDuration > 0) {
                        newGaps.add(new ScheduleGap(gap.getStart(), slotStart, beforeDuration));
                    }
                }
                if (slotEnd.isBefore(gap.getEnd())) {
                    long afterDuration = Duration.between(slotEnd, gap.getEnd()).toMinutes();
                    if (afterDuration > 0) {
                        newGaps.add(new ScheduleGap(slotEnd, gap.getEnd(), afterDuration));
                    }
                }
            } else {
                newGaps.add(gap);
            }
        }
        return newGaps;
    }

    private double computeTemporalPreference(LocalDate date, LocalTime time) {
        long daysFromNow = ChronoUnit.DAYS.between(LocalDate.now(), date);
        double dateScore = Math.max(0, 100.0 * (1.0 - daysFromNow / 14.0));

        double timeScore = 0.0;
        int hour = time.getHour();
        if (hour >= 9 && hour < 12) {
            timeScore = 10.0;
        } else if (hour >= 12 && hour < 17) {
            timeScore = 5.0;
        }

        return dateScore + timeScore;
    }
}
