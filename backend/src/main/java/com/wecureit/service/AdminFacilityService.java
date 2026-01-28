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
import com.wecureit.repository.RoomRepository;
import com.wecureit.repository.SpecialityRepository;
import com.wecureit.dto.response.RoomResponse;



@Service
public class AdminFacilityService {

    private final FacilityRepository facilityRepository;
    private final StateRepository stateRepository;
        private final RoomRepository roomRepository;
        private final SpecialityRepository specialityRepository;

    public AdminFacilityService(
            FacilityRepository facilityRepository,
            StateRepository stateRepository
                        , RoomRepository roomRepository,
                        SpecialityRepository specialityRepository
        ) {
                this.facilityRepository = facilityRepository;
                this.stateRepository = stateRepository;
                this.roomRepository = roomRepository;
                this.specialityRepository = specialityRepository;
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
        f.getIsActive(),
        f.getAddress(),
        f.getZipCode(),
        List.of()
    );
    }

    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findAllByIsActive(Boolean.TRUE)
                .stream()
                .map(f -> {
                    // fetch active rooms for this facility
                    var rooms = roomRepository.findByFacilityIdAndActiveTrue(f.getId());
                    var roomResponses = rooms.stream().map(r -> {
                        String code = r.getSpecialityCode();
                        String name = code;
                        var maybe = specialityRepository.findById(code);
                        if (maybe.isPresent()) name = maybe.get().getSpecialityName();
                        return new RoomResponse(r.getId(), r.getRoomNumber(), code, name, r.getActive());
                    }).toList();

            return new FacilityResponse(
                f.getId(),
                f.getName(),
                f.getCity(),
                f.getState().getStateCode(),
                f.getIsActive(),
                f.getAddress(),
                f.getZipCode(),
                roomResponses
            );
                })
                .toList();
    }

    public void deactivateFacility(java.util.UUID id) {
        var maybe = facilityRepository.findById(id);
        if (maybe.isEmpty()) throw new RuntimeException("Facility not found");
        Facility f = maybe.get();
        f.setIsActive(Boolean.FALSE);
        // also deactivate rooms for good measure
        var rooms = roomRepository.findByFacilityIdAndActiveTrue(f.getId());
        rooms.forEach(r -> r.setActive(false));
        roomRepository.saveAll(rooms);
        facilityRepository.save(f);
    }

    public FacilityResponse updateFacility(java.util.UUID id, com.wecureit.dto.request.UpdateFacilityRequest req) {
        var maybe = facilityRepository.findById(id);
        if (maybe.isEmpty()) throw new RuntimeException("Facility not found");

        Facility f = maybe.get();

        if (req.name != null && !req.name.isBlank()) f.setName(req.name);
        if (req.address != null) f.setAddress(req.address);
        if (req.city != null) f.setCity(req.city);
        if (req.zipCode != null) f.setZipCode(req.zipCode);
        if (req.stateCode != null && !req.stateCode.isBlank()) {
            State s = stateRepository.findById(req.stateCode).orElseThrow(() -> new RuntimeException("Invalid state"));
            f.setState(s);
        }

        facilityRepository.save(f);

        // return updated response including rooms
        var rooms = roomRepository.findByFacilityIdAndActiveTrue(f.getId());
        var roomResponses = rooms.stream().map(r -> {
            String code = r.getSpecialityCode();
            String name = code;
            var maybeSpec = specialityRepository.findById(code);
            if (maybeSpec.isPresent()) name = maybeSpec.get().getSpecialityName();
            return new RoomResponse(r.getId(), r.getRoomNumber(), code, name, r.getActive());
        }).toList();

    return new FacilityResponse(
        f.getId(),
        f.getName(),
        f.getCity(),
        f.getState().getStateCode(),
        f.getIsActive(),
        f.getAddress(),
        f.getZipCode(),
        roomResponses
    );
    }
}

