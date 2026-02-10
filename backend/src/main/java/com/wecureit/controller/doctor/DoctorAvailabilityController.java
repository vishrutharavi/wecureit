package com.wecureit.controller.doctor;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.AvailabilityRequest;
import com.wecureit.dto.response.AvailabilityResponse;
import com.wecureit.service.DoctorAvailabilityService;

@RestController
@RequestMapping("/api/doctors")
public class DoctorAvailabilityController {

    @Autowired
    private DoctorAvailabilityService availabilityService;

    @PostMapping("/{doctorId}/availability")
    public ResponseEntity<List<AvailabilityResponse>> saveAvailability(
        @PathVariable("doctorId") UUID doctorId,
        @RequestBody List<AvailabilityRequest> items
    ) {
        List<AvailabilityResponse> out = availabilityService.saveAvailabilities(doctorId, items);
        return ResponseEntity.ok(out);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{doctorId}/availability")
    public ResponseEntity<List<AvailabilityResponse>> listAvailability(
        @PathVariable("doctorId") UUID doctorId,
        @org.springframework.web.bind.annotation.RequestParam(value = "from", required = false) String from,
        @org.springframework.web.bind.annotation.RequestParam(value = "to", required = false) String to
    ) {
        java.time.LocalDate fromDate = (from == null || from.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(from);
        java.time.LocalDate toDate = (to == null || to.isBlank()) ? fromDate.plusDays(30) : java.time.LocalDate.parse(to);
        List<AvailabilityResponse> out = availabilityService.listAvailabilities(doctorId, fromDate, toDate);
        return ResponseEntity.ok(out);
    }

    // Room assignment and walk-in toggling endpoints removed — rooms are not allocated from the API at this time.

    @org.springframework.web.bind.annotation.DeleteMapping("/{doctorId}/availability/{availabilityId}/delete-availability")
    public ResponseEntity<?> deleteAvailability(
        @PathVariable("doctorId") UUID doctorId,
        @PathVariable("availabilityId") UUID availabilityId
    ) {
        availabilityService.deleteAvailability(doctorId, availabilityId);
        return ResponseEntity.ok().build();
    }
}
