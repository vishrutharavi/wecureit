package com.wecureit.controller.patient;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.BookingDropdownResponse;
import com.wecureit.service.PatientBookingService;

@RestController
@RequestMapping("/api/patients")
public class PatientBookingController {

    private final PatientBookingService bookingService;
    private final com.wecureit.service.AppointmentService appointmentService;

    @Autowired
    public PatientBookingController(PatientBookingService bookingService, com.wecureit.service.AppointmentService appointmentService) {
        this.bookingService = bookingService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/booking/dropdown-data")
    public ResponseEntity<BookingDropdownResponse> getDropdownData(
        @RequestParam(value = "facilityId", required = false) UUID facilityId,
        @RequestParam(value = "specialityCode", required = false) String specialityCode,
        @RequestParam(value = "doctorId", required = false) UUID doctorId
    ) {
        BookingDropdownResponse out = bookingService.getDropdownData(facilityId, specialityCode, doctorId);
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

    /**
     * Maintenance endpoint: recompute breaks for a doctor on a given date.
     * Optionally attempt to associate missing doctor_availability rows before recompute.
     * Example: POST /api/patients/booking/recompute-breaks?doctorId=...&date=2026-02-08&associateMissing=true
     */
    @PostMapping("/booking/recompute-breaks")
    public ResponseEntity<?> recomputeBreaks(
        @RequestParam(value = "doctorId") UUID doctorId,
        @RequestParam(value = "date") String dateStr,
        @RequestParam(value = "associateMissing", required = false, defaultValue = "false") boolean associateMissing
    ) {
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(dateStr);
            // delegate to appointment service helper
            try {
                appointmentService.recomputeBreaksAndAssociateIfNeeded(doctorId, d, associateMissing);
            } catch (Exception ex) {
                return ResponseEntity.status(500).body(java.util.Map.of("error", "Failed to recompute: " + ex.getMessage()));
            }
            return ResponseEntity.ok(java.util.Map.of("status", "ok"));
        } catch (java.time.format.DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
        }
    }
}
