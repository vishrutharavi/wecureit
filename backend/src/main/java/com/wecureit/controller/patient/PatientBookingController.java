package com.wecureit.controller.patient;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.wecureit.dto.request.OptimalSlotRequest;
import com.wecureit.dto.response.BookingDropdownResponse;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.service.OptimalSlotSuggestionService;
import com.wecureit.service.PatientBookingService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientBookingController {

    private final PatientBookingService bookingService;
    private final OptimalSlotSuggestionService optimalSlotService;

    @Autowired
    public PatientBookingController(
            PatientBookingService bookingService,
            OptimalSlotSuggestionService optimalSlotService) {
        this.bookingService = bookingService;
        this.optimalSlotService = optimalSlotService;
    }

    @GetMapping("/booking/dropdown-data")
    public ResponseEntity<BookingDropdownResponse> getDropdownData(
        @RequestParam(value = "facilityId", required = false) UUID facilityId,
        @RequestParam(value = "specialityCode", required = false) String specialityCode,
        @RequestParam(value = "doctorId", required = false) UUID doctorId,
        @RequestParam(value = "workDate", required = false) String workDate
    ) {
        java.time.LocalDate d = null;
        if (workDate != null && !workDate.isBlank()) {
            try { d = java.time.LocalDate.parse(workDate); } catch (java.time.format.DateTimeParseException ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid workDate format, use YYYY-MM-DD"); }
        }
        BookingDropdownResponse out = bookingService.getDropdownData(facilityId, specialityCode, doctorId, d);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/booking/availability")
    public ResponseEntity<?> getBookingAvailability(
        @RequestParam(value = "doctorId") UUID doctorId,
        @RequestParam(value = "facilityId", required = false) UUID facilityId,
        @RequestParam(value = "date") String dateStr
        , @RequestParam(value = "duration", required = false) Integer duration
    ) {
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(dateStr);
            var resp = bookingService.getAvailabilitySlots(doctorId, facilityId, d, duration);
            return ResponseEntity.ok(resp);
        } catch (java.time.format.DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
        }
    }

    @PostMapping("/booking/suggest-optimal-slots")
    public ResponseEntity<?> suggestOptimalSlots(@RequestBody OptimalSlotRequest request) {
        try {
            // Validate inputs
            if (request.getDoctorId() == null || request.getFacilityId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "doctorId and facilityId are required"));
            }

            // Set defaults
            LocalDate startDate = request.getDateRangeStart() != null
                    ? LocalDate.parse(request.getDateRangeStart())
                    : LocalDate.now();

            LocalDate endDate = request.getDateRangeEnd() != null
                    ? LocalDate.parse(request.getDateRangeEnd())
                    : startDate.plusDays(14);

            int duration = request.getDuration() != null ? request.getDuration() : 30;

            // Call service
            List<SlotSuggestion> suggestions = optimalSlotService.suggestOptimalSlots(
                    request.getDoctorId(),
                    request.getFacilityId(),
                    request.getSpecialtyCode(),
                    duration,
                    startDate,
                    endDate
            );

            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to generate suggestions: " + e.getMessage()));
        }
    }

    // recompute-breaks maintenance endpoint removed — recompute is handled asynchronously by appointment events.
}
