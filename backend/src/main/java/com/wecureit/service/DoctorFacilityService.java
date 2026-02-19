package com.wecureit.service;


import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.*;


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
     * Compute availability for a facility at a given date/time range, filtered by speciality.
     * For General Practice: all rooms are eligible.
     * For other specialities: only rooms with matching speciality_code are eligible.
     * A room is occupied if it has an active appointment (with room_id assigned) overlapping the time range.
     */
    public com.wecureit.dto.response.FacilityAvailabilityResponse getFacilityAvailability(UUID facilityId, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        return getFacilityAvailability(facilityId, workDate, startTime, endTime, null);
    }

    public com.wecureit.dto.response.FacilityAvailabilityResponse getFacilityAvailability(UUID facilityId, LocalDate workDate, LocalTime startTime, LocalTime endTime, String specialityCode) {
        List<com.wecureit.entity.Room> eligibleRooms = getEligibleRooms(facilityId, specialityCode);
        int total = eligibleRooms.size();
        int occupied = 0;

        LocalDateTime startAt = LocalDateTime.of(workDate, startTime);
        LocalDateTime endAt = LocalDateTime.of(workDate, endTime);

        // count how many eligible rooms are occupied at this time range
        Set<UUID> occupiedRoomIds = getOccupiedRoomIds(facilityId, startAt, endAt);
        for (com.wecureit.entity.Room r : eligibleRooms) {
            if (occupiedRoomIds.contains(r.getId())) occupied++;
        }

        return new com.wecureit.dto.response.FacilityAvailabilityResponse(facilityId, total, occupied);
    }

    /**
     * Get rooms eligible for a given speciality at a facility.
     * GP → all active rooms. Other speciality → rooms with matching speciality_code.
     */
    public List<com.wecureit.entity.Room> getEligibleRooms(UUID facilityId, String specialityCode) {
        if (specialityCode == null || isGeneralPractice(specialityCode)) {
            List<com.wecureit.entity.Room> rooms = roomRepository.findByFacilityIdAndActiveTrue(facilityId);
            return rooms != null ? rooms : Collections.emptyList();
        }
        List<com.wecureit.entity.Room> rooms = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(facilityId, specialityCode);
        return rooms != null ? rooms : Collections.emptyList();
    }

    /**
     * Get the set of room IDs that are occupied (have active appointments with room_id) in a time range.
     */
    private Set<UUID> getOccupiedRoomIds(UUID facilityId, LocalDateTime startAt, LocalDateTime endAt) {
        Set<UUID> occupiedRoomIds = new HashSet<>();
        try {
            List<com.wecureit.entity.Appointment> overlapping = appointmentRepository.findActiveAppointmentsWithRoomForFacility(facilityId, startAt, endAt);
            if (overlapping != null) {
                for (com.wecureit.entity.Appointment a : overlapping) {
                    if (a.getRoom() != null) {
                        occupiedRoomIds.add(a.getRoom().getId());
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn("getOccupiedRoomIds: error querying appointments: {}", ex.getMessage());
        }
        return occupiedRoomIds;
    }

    /**
     * Find an available room for a speciality at a facility during a time range.
     * For GP: prefer GP-only rooms first, then fallback to any available room.
     * Returns null if no room is available.
     */
    public com.wecureit.entity.Room findAvailableRoom(UUID facilityId, String specialityCode, LocalDateTime startAt, LocalDateTime endAt) {
        List<com.wecureit.entity.Room> eligibleRooms = getEligibleRooms(facilityId, specialityCode);
        if (eligibleRooms.isEmpty()) return null;

        Set<UUID> occupiedRoomIds = getOccupiedRoomIds(facilityId, startAt, endAt);

        boolean isGP = isGeneralPractice(specialityCode);
        if (isGP) {
            // prefer GP-only rooms first to preserve speciality rooms
            String gpCode = getGeneralPracticeCode();
            if (gpCode != null) {
                for (com.wecureit.entity.Room r : eligibleRooms) {
                    if (gpCode.equalsIgnoreCase(r.getSpecialityCode()) && !occupiedRoomIds.contains(r.getId())) {
                        return r;
                    }
                }
            }
        }

        // pick first available eligible room
        for (com.wecureit.entity.Room r : eligibleRooms) {
            if (!occupiedRoomIds.contains(r.getId())) {
                return r;
            }
        }
        return null;
    }

    /**
     * Check if a speciality code represents General Practice.
     */
    public boolean isGeneralPractice(String specialityCode) {
        if (specialityCode == null) return false;
        try {
            var specOpt = specialityRepository.findById(specialityCode);
            if (specOpt.isPresent()) {
                return "general practice".equalsIgnoreCase(specOpt.get().getSpecialityName());
            }
        } catch (Exception ex) {
            // fallback
        }
        return false;
    }

    /**
     * Get the speciality code for General Practice.
     */
    private String getGeneralPracticeCode() {
        try {
            var gpOpt = specialityRepository.findBySpecialityNameIgnoreCase("General Practice");
            if (gpOpt.isPresent()) return gpOpt.get().getSpecialityCode();
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }
}
