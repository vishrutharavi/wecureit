package com.wecureit.controller.doctor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.CreateReferralRequest;
import com.wecureit.dto.response.RecommendedDoctorResponse;
import com.wecureit.dto.response.ReferralResponse;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.SpecialityRepository;
import com.wecureit.service.ReferralService;

@RestController
@RequestMapping("/api/doctors/{doctorId}/referrals")
public class ReferralController {

    private final ReferralService referralService;
    private final SpecialityRepository specialityRepository;

    public ReferralController(ReferralService referralService, SpecialityRepository specialityRepository) {
        this.referralService = referralService;
        this.specialityRepository = specialityRepository;
    }

    @PostMapping
    public ResponseEntity<?> createReferral(
            @PathVariable("doctorId") UUID doctorId,
            @RequestBody CreateReferralRequest request) {
        try {
            ReferralResponse response = referralService.createReferral(doctorId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<ReferralResponse>> getOutgoingReferrals(
            @PathVariable("doctorId") UUID doctorId) {
        return ResponseEntity.ok(referralService.getOutgoingReferrals(doctorId));
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<ReferralResponse>> getIncomingReferrals(
            @PathVariable("doctorId") UUID doctorId) {
        return ResponseEntity.ok(referralService.getIncomingReferrals(doctorId));
    }

    @PatchMapping("/{referralId}/cancel")
    public ResponseEntity<?> cancelReferral(
            @PathVariable("doctorId") UUID doctorId,
            @PathVariable("referralId") UUID referralId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String cancelReason = body != null ? body.get("cancelReason") : null;
            referralService.cancelReferral(referralId, doctorId, cancelReason);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{referralId}/accept")
    public ResponseEntity<?> acceptReferral(
            @PathVariable("doctorId") UUID doctorId,
            @PathVariable("referralId") UUID referralId) {
        try {
            referralService.acceptReferral(referralId, doctorId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{referralId}/complete")
    public ResponseEntity<?> completeReferral(
            @PathVariable("doctorId") UUID doctorId,
            @PathVariable("referralId") UUID referralId) {
        try {
            referralService.completeReferral(referralId, doctorId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendedDoctorResponse>> getRecommendedDoctors(
            @PathVariable("doctorId") UUID doctorId,
            @RequestParam("patientId") UUID patientId,
            @RequestParam("specialityCode") String specialityCode) {
        return ResponseEntity.ok(referralService.getRecommendedDoctors(patientId, specialityCode));
    }

    @GetMapping("/search-doctors")
    public ResponseEntity<List<RecommendedDoctorResponse>> searchDoctors(
            @PathVariable("doctorId") UUID doctorId,
            @RequestParam("query") String query,
            @RequestParam(value = "specialityCode", required = false) String specialityCode) {
        return ResponseEntity.ok(referralService.searchDoctors(query, specialityCode));
    }

    @GetMapping("/specialities")
    public ResponseEntity<List<Speciality>> getSpecialities(
            @PathVariable("doctorId") UUID doctorId) {
        return ResponseEntity.ok(specialityRepository.findAll());
    }
}
