package com.wecureit.controller.patient;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.ReferralResponse;
import com.wecureit.service.ReferralService;

@RestController
@RequestMapping("/api/patient")
public class PatientReferralController {

    private final ReferralService referralService;

    public PatientReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping("/{patientId}/referrals")
    public ResponseEntity<List<ReferralResponse>> getMyReferrals(@PathVariable UUID patientId) {
        return ResponseEntity.ok(referralService.getPatientReferrals(patientId));
    }
}
