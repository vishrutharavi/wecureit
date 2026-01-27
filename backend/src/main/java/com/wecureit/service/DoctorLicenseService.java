package com.wecureit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.response.DoctorLicenseResponse;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.SpecialityRepository;
import com.wecureit.repository.StateRepository;

@Service
public class DoctorLicenseService {

    private final DoctorLicenseRepository repo;
    private final StateRepository stateRepository;
    private final SpecialityRepository specialityRepository;

    public DoctorLicenseService(DoctorLicenseRepository repo, StateRepository stateRepository, SpecialityRepository specialityRepository) {
        this.repo = repo;
        this.stateRepository = stateRepository;
        this.specialityRepository = specialityRepository;
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

    public List<DoctorLicenseResponse> getActiveLicensesForDoctor(UUID doctorId) {
        List<DoctorLicense> rows = repo.findByDoctorIdAndIsActiveTrue(doctorId);
        return rows.stream().map(r -> {
            String stateName = stateRepository.findById(r.getStateCode()).map(s -> s.getStateName()).orElse(r.getStateCode());
            String specialityName = specialityRepository.findById(r.getSpecialityCode()).map(s -> s.getSpecialityName()).orElse(r.getSpecialityCode());
            return new DoctorLicenseResponse(r.getId(), r.getDoctorId(), r.getStateCode(), r.getSpecialityCode(), r.isActive(), stateName, specialityName);
        }).toList();
    }
}

