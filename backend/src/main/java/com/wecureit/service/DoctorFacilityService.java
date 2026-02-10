package com.wecureit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.wecureit.dto.response.FacilityResponse;
import com.wecureit.dto.response.RoomResponse;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Room;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.RoomRepository;

@Service
public class DoctorFacilityService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorFacilityService.class);

    private final FacilityRepository facilityRepository;
    private final RoomRepository roomRepository;
    private final DoctorLicenseRepository licenseRepository;
    private final com.wecureit.repository.SpecialityRepository specialityRepository;
    private final com.wecureit.repository.AppointmentRepository appointmentRepository;

    public DoctorFacilityService(FacilityRepository facilityRepository, RoomRepository roomRepository,
            DoctorLicenseRepository licenseRepository, com.wecureit.repository.SpecialityRepository specialityRepository,
            com.wecureit.repository.AppointmentRepository appointmentRepository) {
        this.facilityRepository = facilityRepository;
        this.roomRepository = roomRepository;
        this.licenseRepository = licenseRepository;
        this.specialityRepository = specialityRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<FacilityResponse> getFacilitiesForDoctor(UUID doctorId) {
        // fetch active licenses for the doctor
        List<DoctorLicense> licenses = licenseRepository.findByDoctorIdAndIsActiveTrue(doctorId);
        logger.info("getFacilitiesForDoctor - doctorId={} licensesFound={}", doctorId, licenses == null ? 0 : licenses.size());
        if (licenses != null && !licenses.isEmpty()) {
            for (DoctorLicense lic : licenses) {
                try {
                    logger.debug("doctorLicense - id={} state={} speciality={} active={}", lic.getId(), lic.getStateCode(), lic.getSpecialityCode(), lic.isActive());
                } catch (Exception e) {
                    logger.debug("doctorLicense - inspect failed: {}", e.getMessage());
                }
            }
        }
        if (licenses == null || licenses.isEmpty()) return Collections.emptyList();

        // map state -> set of speciality codes the doctor holds
        Map<String, Set<String>> stateToSpecs = new HashMap<>();
        for (DoctorLicense dl : licenses) {
            stateToSpecs.computeIfAbsent(dl.getStateCode(), s -> new HashSet<>()).add(dl.getSpecialityCode());
        }

        // fetch all active facilities
        List<Facility> facilities = facilityRepository.findAllByIsActive(Boolean.TRUE);
    logger.debug("getFacilitiesForDoctor - activeFacilitiesCount={}", facilities == null ? 0 : facilities.size());

        List<FacilityResponse> out = new ArrayList<>();

    if (facilities == null) facilities = Collections.emptyList();
    for (Facility f : facilities) {
            String state = null;
            try { state = f.getState() != null ? f.getState().getStateCode() : null; } catch (Exception e) { state = null; }
            if (state == null) {
                logger.debug("skipping facility {} ({}): state missing", f.getId(), f.getName());
                continue;
            }
            Set<String> specs = stateToSpecs.get(state);
            if (specs == null || specs.isEmpty()) {
                logger.debug("skipping facility {} ({}): doctor has no licenses for state {}", f.getId(), f.getName(), state);
                continue;
            }

            // for each speciality the doctor holds in this state, collect rooms
            List<RoomResponse> roomResponses = new ArrayList<>();
            Set<UUID> seen = new HashSet<>();
            for (String spec : specs) {
                List<Room> rooms = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(f.getId(), spec);
                int foundRooms = rooms == null ? 0 : rooms.size();
                logger.debug("facility {} spec {} -> roomsFound={}", f.getId(), spec, foundRooms);
                if (rooms == null || rooms.isEmpty()) continue;
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

            if (roomResponses.isEmpty()) {
                logger.debug("skipping facility {} ({}): no matching active rooms for doctor's licensed specs in state {}", f.getId(), f.getName(), state);
                continue;
            }

            FacilityResponse fr = new FacilityResponse(
                f.getId(), f.getName(), f.getCity(), state, f.getIsActive(), f.getAddress(), f.getZipCode(), roomResponses
            );
            out.add(fr);
        }
        logger.info("getFacilitiesForDoctor - doctorId={} resultFacilities={}", doctorId, out.size());

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
            // compute occupied rooms by counting overlapping appointments in this facility
            try {
                java.util.List<com.wecureit.entity.Appointment> overlapping = appointmentRepository.findAppointmentsForFacility(facilityId, startAt, endAt);
                int occ = 0;
                if (overlapping != null) {
                    for (com.wecureit.entity.Appointment a : overlapping) {
                        if (a.getIsActive() == null || a.getIsActive()) occ++;
                    }
                }
                // cap occupied by total rooms
                occupied = Math.min(total, occ);
            } catch (Exception ex) {
                // fallback: assume zero occupied on error
                occupied = 0;
            }
        }

        return new com.wecureit.dto.response.FacilityAvailabilityResponse(facilityId, total, occupied);
    }
}
