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
        @RequestParam(value = "doctorId", required = false) UUID doctorId
    ) {
        BookingDropdownResponse out = bookingService.getDropdownData(facilityId, specialityCode, doctorId);
        return ResponseEntity.ok(out);
    }
}
