package com.wecureit.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.wecureit.dto.response.SpecialityResponse;
import com.wecureit.repository.SpecialityRepository;

@Service
public class SpecialityService {

    private final SpecialityRepository specialityRepository;

    public SpecialityService(SpecialityRepository specialityRepository) {
        this.specialityRepository = specialityRepository;
    }

    public List<SpecialityResponse> getAllSpecialities() {
        return specialityRepository.findAll()
                .stream()
                .map(s -> new SpecialityResponse(
                        s.getSpecialityCode(),
                        s.getSpecialityName()
                ))
                .toList();
    }
}

