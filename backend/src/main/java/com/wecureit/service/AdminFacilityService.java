package com.wecureit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wecureit.dto.request.CreateFacilityRequest;
import com.wecureit.dto.response.FacilityResponse;
import com.wecureit.entity.Facility;
import com.wecureit.entity.State;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.StateRepository;



@Service
public class AdminFacilityService {

    private final FacilityRepository facilityRepository;
    private final StateRepository stateRepository;

    public AdminFacilityService(
            FacilityRepository facilityRepository,
            StateRepository stateRepository
    ) {
        this.facilityRepository = facilityRepository;
        this.stateRepository = stateRepository;
    }

    public FacilityResponse createFacility(CreateFacilityRequest req) {

        State state = stateRepository.findById(req.stateCode)
                .orElseThrow(() -> new RuntimeException("Invalid state"));

        Facility f = new Facility();
        f.setId(UUID.randomUUID());
        f.setName(req.name);
        f.setAddress(req.address);
        f.setCity(req.city);
        f.setState(state);
        f.setZipCode(req.zipCode);

        facilityRepository.save(f);

        return new FacilityResponse(
                f.getId(),
                f.getName(),
                f.getCity(),
                state.getStateCode(),
                f.getIsActive()
        );
    }

    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findAll()
                .stream()
                .map(f -> new FacilityResponse(
                        f.getId(),
                        f.getName(),
                        f.getCity(),
                        f.getState().getStateCode(),
                        f.getIsActive()
                ))
                .toList();
    }
}

