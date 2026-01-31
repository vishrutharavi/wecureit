package com.wecureit.controller.doctor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.FacilityAvailabilityResponse;
import com.wecureit.dto.response.FacilityResponse;
import com.wecureit.service.DoctorFacilityService;

@RestController
@RequestMapping("/api/doctors")
public class DoctorFacilityController {

    private final DoctorFacilityService facilityService;

    public DoctorFacilityController(DoctorFacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping("/{doctorId}/facilities")
    public ResponseEntity<List<FacilityResponse>> getFacilitiesForDoctor(@PathVariable UUID doctorId) {
        var result = facilityService.getFacilitiesForDoctor(doctorId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{doctorId}/facilities/{facilityId}/availability")
    public ResponseEntity<FacilityAvailabilityResponse> getFacilityAvailability(
            @PathVariable UUID doctorId,
            @PathVariable UUID facilityId,
            @RequestParam(name = "workDate") String workDate,
            @RequestParam(name = "start") String start,
            @RequestParam(name = "end") String end) {

        // parse and delegate to service
        var d = LocalDate.parse(workDate);
        var s = LocalTime.parse(start);
        var e = LocalTime.parse(end);
        FacilityAvailabilityResponse resp = facilityService.getFacilityAvailability(facilityId, d, s, e);
        return ResponseEntity.ok(resp);
    }
}
