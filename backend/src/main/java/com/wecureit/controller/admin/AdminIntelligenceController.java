package com.wecureit.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.BottleneckReport;
import com.wecureit.dto.response.CarePathResponse;
import com.wecureit.dto.response.DoctorBottleneck;
import com.wecureit.dto.response.ReferralPartner;
import com.wecureit.dto.response.ReferralPattern;
import com.wecureit.graph.service.BottleneckAnalyzerService;
import com.wecureit.graph.service.CarePathService;
import com.wecureit.graph.service.GraphSyncService;

@RestController
@RequestMapping("/api/admin/intelligence")
@ConditionalOnProperty(name = "neo4j.enabled", havingValue = "true")
public class AdminIntelligenceController {

    private final BottleneckAnalyzerService bottleneckService;
    private final CarePathService carePathService;
    private final GraphSyncService graphSyncService;

    public AdminIntelligenceController(
            BottleneckAnalyzerService bottleneckService,
            CarePathService carePathService,
            GraphSyncService graphSyncService) {
        this.bottleneckService = bottleneckService;
        this.carePathService = carePathService;
        this.graphSyncService = graphSyncService;
    }

    @GetMapping("/bottlenecks")
    public ResponseEntity<BottleneckReport> getBottleneckReport() {
        return ResponseEntity.ok(bottleneckService.generateReport());
    }

    @GetMapping("/bottlenecks/overloaded")
    public ResponseEntity<List<DoctorBottleneck>> getOverloadedSpecialists(
            @RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(bottleneckService.findOverloadedSpecialists(threshold));
    }

    @GetMapping("/care-path/patient/{patientId}")
    public ResponseEntity<CarePathResponse> getPatientCarePath(@PathVariable UUID patientId) {
        return ResponseEntity.ok(carePathService.getPatientCarePath(patientId));
    }

    @GetMapping("/care-path/doctor/{doctorId}/partners")
    public ResponseEntity<List<ReferralPartner>> getDoctorReferralPartners(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(carePathService.getTopReferralPartners(doctorId));
    }

    @GetMapping("/care-path/patterns")
    public ResponseEntity<List<ReferralPattern>> getCommonPatterns() {
        return ResponseEntity.ok(carePathService.getCommonPatterns());
    }

    @GetMapping("/care-path/shortest")
    public ResponseEntity<CarePathResponse> getShortestPath(
            @RequestParam UUID from, @RequestParam UUID to) {
        return ResponseEntity.ok(carePathService.getShortestPath(from, to));
    }

    @PostMapping("/graph/sync")
    public ResponseEntity<Map<String, String>> triggerFullSync() {
        graphSyncService.syncAllReferrals();
        return ResponseEntity.ok(Map.of("status", "sync_completed"));
    }
}
