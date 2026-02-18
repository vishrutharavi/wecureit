package com.wecureit.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.DoctorReferralStat;
import com.wecureit.dto.response.ReferralOverviewStats;
import com.wecureit.dto.response.ReferralTrendPoint;
import com.wecureit.dto.response.SpecialityStat;
import com.wecureit.service.AdminReferralService;

@RestController
@RequestMapping("/api/admin/referrals")
public class AdminReferralController {

    private final AdminReferralService service;

    public AdminReferralController(AdminReferralService service) {
        this.service = service;
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<ReferralOverviewStats> getOverview() {
        return ResponseEntity.ok(service.getOverviewStats());
    }

    @GetMapping("/stats/trends")
    public ResponseEntity<List<ReferralTrendPoint>> getTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(service.getTrends(days));
    }

    @GetMapping("/stats/by-doctor")
    public ResponseEntity<List<DoctorReferralStat>> getByDoctor() {
        return ResponseEntity.ok(service.getDoctorStats());
    }

    @GetMapping("/stats/by-speciality")
    public ResponseEntity<List<SpecialityStat>> getBySpeciality() {
        return ResponseEntity.ok(service.getSpecialityStats());
    }
}
