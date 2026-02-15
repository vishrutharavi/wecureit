package com.wecureit.service;

import com.wecureit.dto.response.BookingAvailabilityResponse;
import com.wecureit.dto.response.BookingAvailabilitySlot;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.entity.Appointment;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.SpecialityRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OptimalSlotSuggestionService {

    private static final int MAX_NODES_EXPLORED = 500;
    private static final long MAX_EXECUTION_TIME_MS = 2000;
    private static final int MAX_SUGGESTIONS = 5;
    private static final double EXCELLENT_SCORE_THRESHOLD = 80.0;

    private final PatientBookingService patientBookingService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;

    public OptimalSlotSuggestionService(
            PatientBookingService patientBookingService,
            AppointmentRepository appointmentRepository,
            DoctorAvailabilityRepository doctorAvailabilityRepository,
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            SpecialityRepository specialityRepository) {
        this.patientBookingService = patientBookingService;
        this.appointmentRepository = appointmentRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
    }

    /**
     * Inner class representing a gap (idle time) in a doctor's schedule
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Gap {
        private LocalTime start;
        private LocalTime end;
        private long durationMinutes;
    }

    /**
     * Inner class representing a search node for branch and bound
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SearchNode {
        private LocalDate date;
        private LocalTime slotStartTime;
        private LocalTime slotEndTime;
        private int durationMinutes;
        private double actualScore;
        private double boundScore;
        private List<Appointment> existingAppts;
        private List<Gap> gaps;
        private String availabilityId;
        private UUID facilityId;
        private String facilityName;
        private UUID doctorId;
        private String doctorName;
        private String specialty;
        private String specialtyId;
    }

    /**
     * Main entry point for suggesting optimal slots
     */
    public List<SlotSuggestion> suggestOptimalSlots(
            UUID doctorId,
            UUID facilityId,
            String specialtyCode,
            int durationMinutes,
            LocalDate startDate,
            LocalDate endDate) {

        long startTime = System.currentTimeMillis();

        try {
            // Fetch doctor and facility info
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (doctorOpt.isEmpty()) {
                return Collections.emptyList();
            }
            Doctor doctor = doctorOpt.get();

            String facilityName = null;
            if (facilityId != null) {
                Optional<Facility> facilityOpt = facilityRepository.findById(facilityId);
                if (facilityOpt.isPresent()) {
                    facilityName = facilityOpt.get().getName();
                }
            }

            String specialtyName = null;
            if (specialtyCode != null) {
                Optional<Speciality> specialityOpt = specialityRepository.findById(specialtyCode);
                if (specialityOpt.isPresent()) {
                    specialtyName = specialityOpt.get().getSpecialityName();
                }
            }

            // Run branch and bound search
            List<SlotSuggestion> suggestions = branchAndBoundSearch(
                    doctorId,
                    doctor.getName(),
                    facilityId,
                    facilityName,
                    specialtyCode,
                    specialtyName,
                    durationMinutes,
                    startDate,
                    endDate,
                    startTime
            );

            return suggestions;

        } catch (Exception e) {
            // Fallback to empty list on error
            return Collections.emptyList();
        }
    }

    /**
     * Core branch and bound search algorithm
     */
    private List<SlotSuggestion> branchAndBoundSearch(
            UUID doctorId,
            String doctorName,
            UUID facilityId,
            String facilityName,
            String specialtyCode,
            String specialtyName,
            int durationMinutes,
            LocalDate startDate,
            LocalDate endDate,
            long algorithmStartTime) {

        // Priority queue ordered by bound score (descending)
        PriorityQueue<SearchNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(SearchNode::getBoundScore).reversed()
        );

        // Top K solutions (min heap by actual score)
        PriorityQueue<SearchNode> topKSlots = new PriorityQueue<>(
                Comparator.comparingDouble(SearchNode::getActualScore)
        );

        int exploredNodes = 0;

        // Initialize search: iterate through date range
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            try {
                // Fetch available slots for this date
                BookingAvailabilityResponse availability =
                        patientBookingService.getAvailabilitySlots(doctorId, facilityId, currentDate, durationMinutes);

                if (availability != null && availability.getSlots() != null && !availability.getSlots().isEmpty()) {
                    // Fetch existing appointments for this date
                    LocalDateTime dayStart = currentDate.atStartOfDay();
                    LocalDateTime dayEnd = currentDate.atTime(LocalTime.MAX);
                    List<Appointment> existingAppts = appointmentRepository.findAppointmentsForDoctor(
                            doctorId, dayStart, dayEnd
                    );

                    // Detect gaps in the schedule
                    List<Gap> gaps = detectGaps(currentDate, existingAppts);

                    // Create search nodes for each available slot
                    for (BookingAvailabilitySlot slot : availability.getSlots()) {
                        if (!"AVAILABLE".equals(slot.getStatus())) {
                            continue; // Only consider available slots
                        }

                        LocalTime slotTime = LocalTime.parse(slot.getStartAt().substring(11, 16));
                        LocalTime slotEndTime = LocalTime.parse(slot.getEndAt().substring(11, 16));

                        SearchNode node = new SearchNode();
                        node.setDate(currentDate);
                        node.setSlotStartTime(slotTime);
                        node.setSlotEndTime(slotEndTime);
                        node.setDurationMinutes(durationMinutes);
                        node.setExistingAppts(existingAppts);
                        node.setGaps(gaps);
                        node.setAvailabilityId(slot.getAvailabilityId());
                        node.setFacilityId(facilityId);
                        node.setFacilityName(facilityName);
                        node.setDoctorId(doctorId);
                        node.setDoctorName(doctorName);
                        node.setSpecialty(specialtyName);
                        node.setSpecialtyId(specialtyCode);

                        // Compute bound score (optimistic estimate)
                        double boundScore = computeBound(node);
                        node.setBoundScore(boundScore);

                        queue.add(node);
                    }
                }
            } catch (Exception e) {
                // Skip this date if error occurs
            }

            currentDate = currentDate.plusDays(1);
        }

        // Branch and bound search
        while (!queue.isEmpty() && exploredNodes < MAX_NODES_EXPLORED) {
            // Check timeout
            if (System.currentTimeMillis() - algorithmStartTime > MAX_EXECUTION_TIME_MS) {
                break; // Timeout - return best found so far
            }

            SearchNode node = queue.poll();
            exploredNodes++;

            // Pruning: skip if this node's bound is worse than our worst top-K solution
            if (topKSlots.size() >= MAX_SUGGESTIONS) {
                SearchNode worstInTopK = topKSlots.peek();
                if (worstInTopK != null && node.getBoundScore() <= worstInTopK.getActualScore()) {
                    continue; // Prune this branch
                }
            }

            // Compute actual score for this slot
            double actualScore = computeSlotScore(node);
            node.setActualScore(actualScore);

            // Update top-K solutions
            if (topKSlots.size() < MAX_SUGGESTIONS) {
                topKSlots.add(node);
            } else {
                SearchNode worstInTopK = topKSlots.peek();
                if (worstInTopK != null && actualScore > worstInTopK.getActualScore()) {
                    topKSlots.poll();
                    topKSlots.add(node);
                }
            }

            // Early termination if we have excellent solutions
            if (topKSlots.size() == MAX_SUGGESTIONS) {
                boolean allExcellent = topKSlots.stream()
                        .allMatch(n -> n.getActualScore() >= EXCELLENT_SCORE_THRESHOLD);
                if (allExcellent) {
                    break;
                }
            }
        }

        // Convert top-K nodes to SlotSuggestion objects
        return topKSlots.stream()
                .sorted(Comparator.comparingDouble(SearchNode::getActualScore).reversed())
                .map(this::nodeToSlotSuggestion)
                .collect(Collectors.toList());
    }

    /**
     * Detect gaps (idle time) in a doctor's schedule for a given date
     */
    private List<Gap> detectGaps(LocalDate date, List<Appointment> appointments) {
        List<Gap> gaps = new ArrayList<>();

        if (appointments.isEmpty()) {
            // No appointments - entire day is one big gap (handled by availability windows)
            return gaps;
        }

        // Sort appointments by start time
        List<Appointment> sorted = new ArrayList<>(appointments);
        sorted.sort(Comparator.comparing(Appointment::getStartTime));

        // Find gaps between consecutive appointments
        for (int i = 0; i < sorted.size() - 1; i++) {
            LocalDateTime currentEnd = sorted.get(i).getEndTime();
            LocalDateTime nextStart = sorted.get(i + 1).getStartTime();

            if (currentEnd.isBefore(nextStart)) {
                long gapMinutes = Duration.between(currentEnd, nextStart).toMinutes();
                if (gapMinutes > 0) {
                    gaps.add(new Gap(
                            currentEnd.toLocalTime(),
                            nextStart.toLocalTime(),
                            gapMinutes
                    ));
                }
            }
        }

        return gaps;
    }

    /**
     * Compute upper bound score for a node (optimistic estimate)
     */
    private double computeBound(SearchNode node) {
        // Best case: assume we find a slot that perfectly fills a gap
        double maxGapFill = 100.0;

        // Best case: no fragmentation penalty
        double minFragmentation = 0.0;

        // Temporal score for current date
        double temporalScore = computeTemporalPreference(node.getDate(), node.getSlotStartTime());

        return 0.5 * maxGapFill + 0.3 * minFragmentation + 0.2 * temporalScore;
    }

    /**
     * Compute actual score for a slot
     */
    private double computeSlotScore(SearchNode node) {
        double gapFillScore = computeGapFillScore(node.getSlotStartTime(), node.getSlotEndTime(),
                node.getDurationMinutes(), node.getGaps(), node.getExistingAppts());

        double fragmentationPenalty = computeFragmentationPenalty(node.getSlotStartTime(), node.getSlotEndTime(),
                node.getDurationMinutes(), node.getGaps());

        double temporalScore = computeTemporalPreference(node.getDate(), node.getSlotStartTime());

        return 0.5 * gapFillScore + 0.3 * fragmentationPenalty + 0.2 * temporalScore;
    }

    /**
     * Compute gap-filling score
     */
    private double computeGapFillScore(LocalTime slotStart, LocalTime slotEnd, int duration,
                                       List<Gap> gaps, List<Appointment> existingAppts) {
        // Check if slot fills an existing gap
        for (Gap gap : gaps) {
            // Perfect fit: slot exactly fills the gap
            if (slotStart.equals(gap.getStart()) && slotEnd.equals(gap.getEnd())) {
                return 100.0;
            }

            // Good fit: slot fills beginning or end of gap cleanly
            if (slotStart.equals(gap.getStart()) || slotEnd.equals(gap.getEnd())) {
                return 80.0;
            }

            // Acceptable fit: slot is within gap
            if (!slotStart.isBefore(gap.getStart()) && !slotEnd.isAfter(gap.getEnd())) {
                long remainingGap = gap.getDurationMinutes() - duration;
                if (remainingGap >= 60) {
                    return 70.0; // Leaves large usable gap
                } else if (remainingGap >= 30) {
                    return 60.0; // Leaves medium usable gap
                } else if (remainingGap >= 15) {
                    return 40.0; // Leaves small gap
                } else {
                    return 20.0; // Creates unusable fragment
                }
            }
        }

        // Check if adjacent to existing appointment
        if (isAdjacentToAppointment(slotStart, slotEnd, existingAppts)) {
            return 30.0;
        }

        // Isolated slot (no existing appointments) - still valuable
        if (existingAppts.isEmpty()) {
            return 50.0;
        }

        return 10.0;
    }

    /**
     * Check if slot is adjacent to an existing appointment
     */
    private boolean isAdjacentToAppointment(LocalTime slotStart, LocalTime slotEnd,
                                            List<Appointment> existingAppts) {
        for (Appointment appt : existingAppts) {
            LocalTime apptStart = appt.getStartTime().toLocalTime();
            LocalTime apptEnd = appt.getEndTime().toLocalTime();

            // Adjacent if slot ends where appointment starts, or appointment ends where slot starts
            if (slotEnd.equals(apptStart) || apptEnd.equals(slotStart)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute fragmentation penalty
     */
    private double computeFragmentationPenalty(LocalTime slotStart, LocalTime slotEnd, int duration,
                                               List<Gap> existingGaps) {
        // Simulate inserting this slot and compute resulting gaps
        List<Gap> simulatedGaps = simulateSlotInsertion(slotStart, slotEnd, duration, existingGaps);

        double penalty = 0.0;

        for (Gap gap : simulatedGaps) {
            if (gap.getDurationMinutes() < 15) {
                penalty -= 50.0; // Completely unusable
            } else if (gap.getDurationMinutes() < 30) {
                penalty -= 20.0; // Only for 15-min appointments
            } else if (gap.getDurationMinutes() < 60) {
                penalty -= 10.0; // Limits flexibility
            }
            // Gaps >= 60 minutes: no penalty
        }

        return penalty;
    }

    /**
     * Simulate inserting a slot into existing gaps
     */
    private List<Gap> simulateSlotInsertion(LocalTime slotStart, LocalTime slotEnd, int duration,
                                            List<Gap> existingGaps) {
        List<Gap> newGaps = new ArrayList<>();

        for (Gap gap : existingGaps) {
            // Check if slot is within this gap
            if (!slotStart.isBefore(gap.getStart()) && !slotEnd.isAfter(gap.getEnd())) {
                // Slot splits this gap into two parts

                // Gap before slot
                if (slotStart.isAfter(gap.getStart())) {
                    long beforeDuration = Duration.between(gap.getStart(), slotStart).toMinutes();
                    if (beforeDuration > 0) {
                        newGaps.add(new Gap(gap.getStart(), slotStart, beforeDuration));
                    }
                }

                // Gap after slot
                if (slotEnd.isBefore(gap.getEnd())) {
                    long afterDuration = Duration.between(slotEnd, gap.getEnd()).toMinutes();
                    if (afterDuration > 0) {
                        newGaps.add(new Gap(slotEnd, gap.getEnd(), afterDuration));
                    }
                }
            } else {
                // Gap is not affected by this slot
                newGaps.add(gap);
            }
        }

        return newGaps;
    }

    /**
     * Compute temporal preference score (earlier is better)
     */
    private double computeTemporalPreference(LocalDate date, LocalTime time) {
        // Date preference: earlier dates are better
        long daysFromNow = ChronoUnit.DAYS.between(LocalDate.now(), date);
        double dateScore = Math.max(0, 100.0 * (1.0 - daysFromNow / 14.0)); // 14-day window

        // Time preference: morning slots slightly preferred
        double timeScore = 0.0;
        int hour = time.getHour();
        if (hour >= 9 && hour < 12) {
            timeScore = 10.0; // Morning bonus
        } else if (hour >= 12 && hour < 17) {
            timeScore = 5.0; // Afternoon
        }

        return dateScore + timeScore;
    }

    /**
     * Convert SearchNode to SlotSuggestion
     */
    private SlotSuggestion nodeToSlotSuggestion(SearchNode node) {
        String reason = buildReason(node);

        return new SlotSuggestion(
                node.getDoctorId() != null ? node.getDoctorId().toString() : null,
                node.getDoctorName(),
                node.getSpecialty(),
                node.getSpecialtyId(),
                node.getFacilityId() != null ? node.getFacilityId().toString() : null,
                node.getFacilityName(),
                node.getDate().toString(),
                node.getSlotStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                node.getSlotEndTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                node.getDurationMinutes(),
                reason,
                node.getAvailabilityId()
        );
    }

    /**
     * Build human-readable reason for slot suggestion
     */
    private String buildReason(SearchNode node) {
        double score = node.getActualScore();
        LocalTime slotStart = node.getSlotStartTime();
        LocalTime slotEnd = node.getSlotEndTime();
        List<Gap> gaps = node.getGaps();
        List<Appointment> appts = node.getExistingAppts();

        // Check if it fills a gap
        for (Gap gap : gaps) {
            if (slotStart.equals(gap.getStart()) && slotEnd.equals(gap.getEnd())) {
                return String.format("Perfectly fills %d-min gap at %s (Score: %.1f)",
                        gap.getDurationMinutes(),
                        slotStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                        score);
            }

            if (slotStart.equals(gap.getStart()) || slotEnd.equals(gap.getEnd())) {
                return String.format("Fills gap cleanly at %s (Score: %.1f)",
                        slotStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                        score);
            }

            if (!slotStart.isBefore(gap.getStart()) && !slotEnd.isAfter(gap.getEnd())) {
                return String.format("Fills %d-min gap between appointments (Score: %.1f)",
                        gap.getDurationMinutes(),
                        score);
            }
        }

        // Check if adjacent to appointment
        if (isAdjacentToAppointment(slotStart, slotEnd, appts)) {
            return String.format("Adjacent to existing appointment at %s (Score: %.1f)",
                    slotStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                    score);
        }

        // Default reason
        if (appts.isEmpty()) {
            return String.format("Earliest available slot at %s (Score: %.1f)",
                    slotStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                    score);
        }

        return String.format("Optimal slot at %s (Score: %.1f)",
                slotStart.format(DateTimeFormatter.ofPattern("h:mm a")),
                score);
    }
}
