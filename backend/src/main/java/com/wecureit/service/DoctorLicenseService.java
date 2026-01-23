package com.wecureit.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.entity.DoctorLicense;
import com.wecureit.repository.DoctorLicenseRepository;

@Service
public class DoctorLicenseService {

    private final DoctorLicenseRepository repo;

    public DoctorLicenseService(DoctorLicenseRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public DoctorLicense addLicense(UUID doctorId, String stateCode, String specialityCode) {

        return repo.findAll().stream()
            .filter(dl ->
                dl.getDoctorId().equals(doctorId) &&
                dl.getStateCode().equalsIgnoreCase(stateCode) &&
                dl.getSpecialityCode().equalsIgnoreCase(specialityCode)
            )
            .findFirst()
            .map(existing -> {
                existing.setActive(true);
                return repo.save(existing);
            })
            .orElseGet(() -> {
                DoctorLicense dl = new DoctorLicense();
                dl.setDoctorId(doctorId);
                dl.setStateCode(stateCode.toUpperCase());
                dl.setSpecialityCode(specialityCode.toUpperCase());
                dl.setActive(true);
                return repo.save(dl);
            });
    }

    @Transactional
    public void deactivateLicense(UUID licenseId) {
        DoctorLicense license = repo.findById(licenseId)
            .orElseThrow(() -> new IllegalArgumentException("License not found"));

        license.setActive(false);
        repo.save(license);
    }

    public java.util.List<DoctorLicense> getActiveLicensesForDoctor(UUID doctorId) {
        return repo.findByDoctorIdAndIsActiveTrue(doctorId);
    }
}

