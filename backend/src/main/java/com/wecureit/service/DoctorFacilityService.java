package com.wecureit.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.wecureit.dto.response.FacilityResponse;
import com.wecureit.dto.response.RoomResponse;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Room;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.RoomRepository;
import com.wecureit.repository.DoctorLicenseRepository;

@Service
public class DoctorFacilityService {

    private final FacilityRepository facilityRepository;
    private final RoomRepository roomRepository;
    private final DoctorLicenseRepository licenseRepository;
    private final com.wecureit.repository.RoomScheduleRepository roomScheduleRepository;
    private final com.wecureit.repository.SpecialityRepository specialityRepository;

    public DoctorFacilityService(FacilityRepository facilityRepository, RoomRepository roomRepository,
            DoctorLicenseRepository licenseRepository, com.wecureit.repository.RoomScheduleRepository roomScheduleRepository,
            com.wecureit.repository.SpecialityRepository specialityRepository) {
        this.facilityRepository = facilityRepository;
        this.roomRepository = roomRepository;
        this.licenseRepository = licenseRepository;
        this.roomScheduleRepository = roomScheduleRepository;
        this.specialityRepository = specialityRepository;
    }

    public List<FacilityResponse> getFacilitiesForDoctor(UUID doctorId) {
        // fetch active licenses for the doctor
        List<DoctorLicense> licenses = licenseRepository.findByDoctorIdAndIsActiveTrue(doctorId);
        if (licenses == null || licenses.isEmpty()) return Collections.emptyList();

        // map state -> set of speciality codes the doctor holds
        Map<String, Set<String>> stateToSpecs = new HashMap<>();
        for (DoctorLicense dl : licenses) {
            stateToSpecs.computeIfAbsent(dl.getStateCode(), s -> new HashSet<>()).add(dl.getSpecialityCode());
        }

        // fetch all active facilities
        List<Facility> facilities = facilityRepository.findAllByIsActive(Boolean.TRUE);

        List<FacilityResponse> out = new ArrayList<>();

        for (Facility f : facilities) {
            String state = f.getState() != null ? f.getState().getStateCode() : null;
            if (state == null) continue;
            Set<String> specs = stateToSpecs.get(state);
            if (specs == null || specs.isEmpty()) continue;

            // for each speciality the doctor holds in this state, collect rooms
            List<RoomResponse> roomResponses = new ArrayList<>();
            Set<UUID> seen = new HashSet<>();
            for (String spec : specs) {
                List<Room> rooms = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(f.getId(), spec);
                for (Room r : rooms) {
                    if (seen.add(r.getId())) {
                        String specialtyName = spec;
                        try {
                            var maybe = specialityRepository.findById(spec);
                            if (maybe.isPresent()) specialtyName = maybe.get().getSpecialityName();
                        } catch (Exception ex) {
                            // fallback to code
                        }
                        RoomResponse rr = new RoomResponse(r.getId(), r.getRoomNumber(), r.getSpecialityCode(), specialtyName, r.getActive());
                        roomResponses.add(rr);
                    }
                }
            }

            if (!roomResponses.isEmpty()) {
                FacilityResponse fr = new FacilityResponse(
                    f.getId(), f.getName(), f.getCity(), f.getState().getStateCode(), f.getIsActive(), f.getAddress(), f.getZipCode(), roomResponses
                );
                out.add(fr);
            }
        }

        return out;
    }

    /**
     * Compute availability for a facility at a given date/time range.
     * Only schedules that are tied to an appointment (appointmentId != null) are
     * considered occupied. Room reservations for availability without an
     * appointment do not reduce the available room count (they are considered
     * eligible for walk-in behavior).
     */
    public com.wecureit.dto.response.FacilityAvailabilityResponse getFacilityAvailability(java.util.UUID facilityId, java.time.LocalDate workDate, java.time.LocalTime startTime, java.time.LocalTime endTime) {
        java.util.List<com.wecureit.entity.Room> rooms = roomRepository.findByFacilityIdAndActiveTrue(facilityId);
        int total = rooms == null ? 0 : rooms.size();
        int occupied = 0;

        java.time.LocalDateTime startAt = java.time.LocalDateTime.of(workDate, startTime);
        java.time.LocalDateTime endAt = java.time.LocalDateTime.of(workDate, endTime);

        if (rooms != null) {
            for (com.wecureit.entity.Room r : rooms) {
                java.util.List<com.wecureit.entity.RoomSchedule> overlaps = roomScheduleRepository.findOverlapping(r.getId(), startAt, endAt);
                boolean hasAppointment = false;
                if (overlaps != null) {
                    for (com.wecureit.entity.RoomSchedule rs : overlaps) {
                        if (rs.getAppointmentId() != null) { hasAppointment = true; break; }
                    }
                }
                if (hasAppointment) occupied++;
            }
        }

        return new com.wecureit.dto.response.FacilityAvailabilityResponse(facilityId, total, occupied);
    }
}
