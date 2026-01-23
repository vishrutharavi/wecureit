package com.wecureit.controller.admin;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.AddDoctorLicenseRequest;
import com.wecureit.service.DoctorLicenseService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/doctor-licenses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorLicenseController {

    private final DoctorLicenseService service;

    public AdminDoctorLicenseController(DoctorLicenseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> addLicense(@Valid @RequestBody AddDoctorLicenseRequest req) {
        service.addLicense(req.getDoctorId(), req.getStateCode(), req.getSpecialityCode());
        return ResponseEntity.ok(Map.of("message", "License added"));
    }

    @GetMapping
    public ResponseEntity<?> getLicenses(@RequestParam("doctorId") UUID doctorId) {
        var list = service.getActiveLicensesForDoctor(doctorId);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{licenseId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable UUID licenseId) {
        service.deactivateLicense(licenseId);
        return ResponseEntity.ok(Map.of("message", "License deactivated"));
    }
}
