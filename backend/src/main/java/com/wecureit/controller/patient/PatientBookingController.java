package com.wecureit.controller.patient;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.BookingDropdownResponse;
import com.wecureit.service.PatientBookingService;

@RestController
@RequestMapping("/api/patients")
public class PatientBookingController {

    private final PatientBookingService bookingService;
    

    @Autowired
    public PatientBookingController(PatientBookingService bookingService) {
        this.bookingService = bookingService;
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

    // recompute-breaks maintenance endpoint removed — recompute is handled asynchronously by appointment events.
}
