package com.wecureit.service;

import com.wecureit.dto.response.BookingAvailabilityResponse;
import com.wecureit.dto.response.BookingAvailabilitySlot;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.entity.Appointment;
import com.wecureit.model.ScheduleGap;
import com.wecureit.model.SlotSearchNode;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.SpecialityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OptimalSlotSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(OptimalSlotSuggestionService.class);

    private static final int MAX_NODES_EXPLORED = 500;
    private static final long MAX_SEARCH_TIME_MS = 5000;
    private static final int MAX_SUGGESTIONS = 5;
    private static final double EXCELLENT_SCORE_THRESHOLD = 80.0;
    private final PatientBookingService patientBookingService;
    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;
    private final ScheduleGapAnalyzer gapAnalyzer;
    private final SlotValidationService validationService;
    private final SlotScoringService scoringService;

    public OptimalSlotSuggestionService(
            PatientBookingService patientBookingService,
            AppointmentRepository appointmentRepository,
            DoctorAvailabilityRepository doctorAvailabilityRepository,
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            SpecialityRepository specialityRepository,
            ScheduleGapAnalyzer gapAnalyzer,
            SlotValidationService validationService,
            SlotScoringService scoringService) {
        this.patientBookingService = patientBookingService;
        this.appointmentRepository = appointmentRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
        this.gapAnalyzer = gapAnalyzer;
        this.validationService = validationService;
        this.scoringService = scoringService;
    }

    /**
     * Main entry point for suggesting optimal slots.
     */
    public List<SlotSuggestion> suggestOptimalSlots(
            UUID doctorId,
            UUID facilityId,
            String specialtyCode,
            int durationMinutes,
            LocalDate startDate,
            LocalDate endDate) {

        logger.info("suggestOptimalSlots - doctorId={}, facilityId={}, specialty={}, duration={}, range={} to {}",
                doctorId, facilityId, specialtyCode, durationMinutes, startDate, endDate);

        long startTime = System.currentTimeMillis();

        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
            if (doctorOpt.isEmpty()) {
                logger.warn("suggestOptimalSlots - doctor not found: {}", doctorId);
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

            List<SlotSuggestion> suggestions = branchAndBoundSearch(
                    doctorId, doctor.getName(), facilityId, facilityName,
                    specialtyCode, specialtyName, durationMinutes, startDate, endDate, startTime);

            logger.info("suggestOptimalSlots - returning {} suggestions in {}ms",
                    suggestions.size(), System.currentTimeMillis() - startTime);
            return suggestions;

        } catch (Exception e) {
            logger.error("suggestOptimalSlots - unexpected error for doctorId={}: {}", doctorId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Core branch and bound search algorithm.
     */
    private List<SlotSuggestion> branchAndBoundSearch(
            UUID doctorId, String doctorName,
            UUID facilityId, String facilityName,
            String specialtyCode, String specialtyName,
            int durationMinutes, LocalDate startDate, LocalDate endDate,
            long algorithmStartTime) {

        PriorityQueue<SlotSearchNode> queue = new PriorityQueue<>(
                Comparator.comparingDouble(SlotSearchNode::getBoundScore).reversed());
        PriorityQueue<SlotSearchNode> topKSlots = new PriorityQueue<>(
                Comparator.comparingDouble(SlotSearchNode::getActualScore));

        // Phase 1: Build candidate queue from all dates
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            try {
                int candidatesForDate = buildCandidatesForDate(
                        currentDate, doctorId, doctorName, facilityId, facilityName,
                        specialtyCode, specialtyName, durationMinutes, queue);

                logger.debug("Date {} - {} valid candidates added to queue", currentDate, candidatesForDate);

            } catch (Exception e) {
                logger.warn("Error processing date {} for doctorId={}: {}", currentDate, doctorId, e.getMessage(), e);
            }
            currentDate = currentDate.plusDays(1);
        }

        long initDurationMs = System.currentTimeMillis() - algorithmStartTime;
        logger.info("B&B search - total candidates in queue: {}, init took {}ms", queue.size(), initDurationMs);

        if (queue.isEmpty()) {
            logger.warn("B&B search - no valid candidates found across date range {}-{}", startDate, endDate);
            return Collections.emptyList();
        }

        // Phase 2: Branch and bound search (timeout starts here, not during init)
        long searchStartTime = System.currentTimeMillis();
        int exploredNodes = 0;
        while (!queue.isEmpty() && exploredNodes < MAX_NODES_EXPLORED) {
            if (System.currentTimeMillis() - searchStartTime > MAX_SEARCH_TIME_MS) {
                logger.info("B&B search - timeout after {}ms, explored {} nodes",
                        System.currentTimeMillis() - searchStartTime, exploredNodes);
                break;
            }

            SlotSearchNode node = queue.poll();
            exploredNodes++;

            // Pruning
            if (topKSlots.size() >= MAX_SUGGESTIONS) {
                SlotSearchNode worstInTopK = topKSlots.peek();
                if (worstInTopK != null && node.getBoundScore() <= worstInTopK.getActualScore()) {
                    continue;
                }
            }

            double actualScore = scoringService.computeSlotScore(node);
            node.setActualScore(actualScore);

            if (topKSlots.size() < MAX_SUGGESTIONS) {
                topKSlots.add(node);
            } else {
                SlotSearchNode worstInTopK = topKSlots.peek();
                if (worstInTopK != null && actualScore > worstInTopK.getActualScore()) {
                    topKSlots.poll();
                    topKSlots.add(node);
                }
            }

            // Early termination if all solutions are excellent
            if (topKSlots.size() == MAX_SUGGESTIONS) {
                boolean allExcellent = topKSlots.stream()
                        .allMatch(n -> n.getActualScore() >= EXCELLENT_SCORE_THRESHOLD);
                if (allExcellent) {
                    logger.debug("B&B search - early termination, all {} solutions are excellent", MAX_SUGGESTIONS);
                    break;
                }
            }
        }

        logger.debug("B&B search - explored {} nodes, found {} candidates", exploredNodes, topKSlots.size());

        // Phase 3: Deduplicate and convert
        return deduplicateAndConvert(topKSlots);
    }

    /**
     * Build candidate nodes for a single date and add them to the queue.
     */
    private int buildCandidatesForDate(
            LocalDate date, UUID doctorId, String doctorName,
            UUID facilityId, String facilityName,
            String specialtyCode, String specialtyName,
            int durationMinutes, PriorityQueue<SlotSearchNode> queue) {

        BookingAvailabilityResponse availability =
                patientBookingService.getAvailabilitySlots(doctorId, facilityId, date, durationMinutes, specialtyCode);

        if (availability == null || availability.getSlots() == null || availability.getSlots().isEmpty()) {
            logger.debug("Date {} - no availability slots returned", date);
            return 0;
        }

        logger.debug("Date {} - {} slots from availability API", date, availability.getSlots().size());

        // Fetch existing appointments
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        List<Appointment> existingAppts = appointmentRepository.findAppointmentsForDoctor(doctorId, dayStart, dayEnd);
        List<Appointment> activeAppts = validationService.filterActiveAppointments(existingAppts);

        // Parse slots and build unbooked set
        Set<LocalTime> unbookedSlotTimes = new HashSet<>();
        Map<LocalTime, String> slotAvailabilityIdMap = new HashMap<>();
        LocalTime officeStart = null;
        LocalTime officeEnd = null;

        for (BookingAvailabilitySlot slot : availability.getSlots()) {
            LocalTime st = parseSlotTime(slot.getStartAt());
            LocalTime et = parseSlotTime(slot.getEndAt());
            if (st == null || et == null) {
                logger.warn("Date {} - failed to parse slot time: startAt={}, endAt={}", date, slot.getStartAt(), slot.getEndAt());
                continue;
            }

            if (!"BOOKED".equals(slot.getStatus()) && !"UNAVAILABLE".equals(slot.getStatus())
                    && !"BREAK_ENFORCED".equals(slot.getStatus())) {
                unbookedSlotTimes.add(st);
            }

            if (slot.getAvailabilityId() != null) {
                slotAvailabilityIdMap.putIfAbsent(st, slot.getAvailabilityId());
            }

            if (officeStart == null || st.isBefore(officeStart)) officeStart = st;
            if (officeEnd == null || et.isAfter(officeEnd)) officeEnd = et;
        }

        if (unbookedSlotTimes.isEmpty()) {
            logger.debug("Date {} - all slots are booked/unavailable", date);
            return 0;
        }

        logger.debug("Date {} - {} unbooked slots, {} active appointments, office {}-{}",
                date, unbookedSlotTimes.size(), activeAppts.size(), officeStart, officeEnd);

        // Detect gaps
        List<ScheduleGap> gaps = gapAnalyzer.detectGaps(date, existingAppts, officeStart, officeEnd);

        // Validate and create nodes for each candidate
        int candidateCount = 0;
        for (LocalTime candidateStart : unbookedSlotTimes) {
            if (!validationService.isValidCandidate(date, candidateStart, durationMinutes, unbookedSlotTimes, activeAppts)) {
                continue;
            }

            LocalTime slotEndTime = candidateStart.plusMinutes(durationMinutes);

            SlotSearchNode node = new SlotSearchNode();
            node.setDate(date);
            node.setSlotStartTime(candidateStart);
            node.setSlotEndTime(slotEndTime);
            node.setDurationMinutes(durationMinutes);
            node.setExistingAppts(existingAppts);
            node.setGaps(gaps);
            node.setAvailabilityId(slotAvailabilityIdMap.get(candidateStart));
            node.setFacilityId(facilityId);
            node.setFacilityName(facilityName);
            node.setDoctorId(doctorId);
            node.setDoctorName(doctorName);
            node.setSpecialty(specialtyName);
            node.setSpecialtyId(specialtyCode);

            double boundScore = scoringService.computeBound(node);
            node.setBoundScore(boundScore);

            queue.add(node);
            candidateCount++;
        }

        return candidateCount;
    }

    /**
     * Parse time from a datetime string. Handles both ISO format and HH:mm substring.
     */
    private LocalTime parseSlotTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            // Try ISO datetime parse first
            if (dateTimeStr.length() >= 16) {
                return LocalTime.parse(dateTimeStr.substring(11, 16));
            }
            // Fallback: try parsing as time directly
            return LocalTime.parse(dateTimeStr);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            logger.warn("Failed to parse slot time from '{}': {}", dateTimeStr, e.getMessage());
            return null;
        }
    }

    /**
     * Deduplicate overlapping suggestions and convert to SlotSuggestion DTOs.
     */
    private List<SlotSuggestion> deduplicateAndConvert(PriorityQueue<SlotSearchNode> topKSlots) {
        List<SlotSearchNode> sorted = topKSlots.stream()
                .sorted(Comparator.comparingDouble(SlotSearchNode::getActualScore).reversed())
                .collect(Collectors.toList());

        List<SlotSearchNode> deduplicated = new ArrayList<>();
        for (SlotSearchNode candidate : sorted) {
            boolean overlapsExisting = deduplicated.stream().anyMatch(accepted ->
                    candidate.getDate().equals(accepted.getDate())
                            && candidate.getSlotStartTime().isBefore(accepted.getSlotEndTime())
                            && candidate.getSlotEndTime().isAfter(accepted.getSlotStartTime()));
            if (!overlapsExisting) {
                deduplicated.add(candidate);
                if (deduplicated.size() >= MAX_SUGGESTIONS) break;
            }
        }

        logger.debug("Deduplication: {} candidates -> {} non-overlapping suggestions",
                sorted.size(), deduplicated.size());

        return deduplicated.stream()
                .map(this::nodeToSlotSuggestion)
                .collect(Collectors.toList());
    }

    private SlotSuggestion nodeToSlotSuggestion(SlotSearchNode node) {
        String reason = scoringService.buildReason(node);

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
}
