package com.wecureit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.request.AvailabilityRequest;
import com.wecureit.dto.response.AvailabilityResponse;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Room;
import com.wecureit.entity.RoomSchedule;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.RoomRepository;
import com.wecureit.repository.RoomScheduleRepository;

@Service
public class DoctorAvailabilityService {

    @Autowired
    private DoctorAvailabilityRepository availabilityRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private RoomScheduleRepository roomScheduleRepo;

    @Autowired
    private FacilityRepository facilityRepo;

    @Autowired
    private DoctorLicenseRepository doctorLicenseRepo;

    @Autowired
    private DoctorFacilityService doctorFacilityService;

    @Transactional
    public List<AvailabilityResponse> saveAvailabilities(UUID doctorId, List<AvailabilityRequest> items) {
        List<AvailabilityResponse> results = new ArrayList<>();

        for (AvailabilityRequest it : items) {
            // parse incoming workDate as local date (accept either YYYY-MM-DD or ISO datetime)
            LocalDate workDate = null;
            if (it.getWorkDate() != null && !it.getWorkDate().isBlank()) {
                String raw = it.getWorkDate();
                // if contains 'T' (ISO datetime), take date part before 'T'
                if (raw.contains("T")) raw = raw.split("T")[0];
                workDate = LocalDate.parse(raw);
            } else {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "workDate is required");
            }
            // server-side validations
            LocalTime s = LocalTime.parse(it.getStartTime());
            LocalTime e = LocalTime.parse(it.getEndTime());
            int minutes = Math.max(0, (int) java.time.Duration.between(s, e).toMinutes());
            if (minutes < 240) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Minimum 4 hours required for availability");
            }

            // parse facility id early
            UUID facId = UUID.fromString(it.getFacilityId());

            // prevent duplicate availability for the same doctor/facility/date/start/end
            if (availabilityRepo.existsByDoctorIdAndFacilityIdAndWorkDateAndStartTimeAndEndTime(doctorId, facId, workDate, LocalTime.parse(it.getStartTime()), LocalTime.parse(it.getEndTime()))) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Duplicate availability for same facility and time");
            }

            // check doctor license for speciality in facility's state
            if (it.getSpecialityCode() != null && !it.getSpecialityCode().isEmpty()) {
                Facility fac = facilityRepo.findById(facId).orElse(null);
                if (fac == null) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Facility not found");
                }
                String stateCode = fac.getState().getStateCode();
                boolean hasLicense = doctorLicenseRepo.existsByDoctorIdAndStateCodeAndSpecialityCodeAndIsActiveTrue(doctorId, stateCode, it.getSpecialityCode());
                if (!hasLicense) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Doctor does not have an active license for this speciality in facility's state");
                }
            }

            DoctorAvailability da = new DoctorAvailability();
            da.setDoctorId(doctorId);
            da.setFacilityId(UUID.fromString(it.getFacilityId()));
            da.setWorkDate(workDate);
            da.setStartTime(LocalTime.parse(it.getStartTime()));
            da.setEndTime(LocalTime.parse(it.getEndTime()));
            da.setSpecialityCode(it.getSpecialityCode());
            // initially pending
            da.setRoomAssignmentStatus("PENDING");
            da.setIsBookable(true);
            availabilityRepo.save(da);

            // attempt to find a room and reserve via room_schedule
            List<Room> candidates = roomRepo.findByFacilityIdAndSpecialityCodeAndActiveTrue(da.getFacilityId(), da.getSpecialityCode());
            UUID assignedRoomId = null;
            LocalDateTime startAt = LocalDateTime.of(da.getWorkDate(), da.getStartTime());
            LocalDateTime endAt = LocalDateTime.of(da.getWorkDate(), da.getEndTime());

            for (Room r : candidates) {
                try {
                    RoomSchedule rs = new RoomSchedule();
                    rs.setRoomId(r.getId());
                    rs.setDoctorId(doctorId);
                    rs.setStartAt(startAt);
                    rs.setEndAt(endAt);
                    rs.setPurpose("AVAILABILITY_ASSIGN");
                    roomScheduleRepo.save(rs);
                    assignedRoomId = r.getId();
                    break;
                } catch (DataIntegrityViolationException ex) {
                    // room busy, try next
                    continue;
                }
            }

            if (assignedRoomId != null) {
                da.setRoomAssignedId(assignedRoomId);
                da.setRoomAssignmentStatus("ASSIGNED");
                da.setIsBookable(true);
                availabilityRepo.save(da);
            } else {
                da.setRoomAssignmentStatus("NONE");
                da.setIsBookable(false);
                availabilityRepo.save(da);
            }

            AvailabilityResponse resp = new AvailabilityResponse();
            resp.setId(da.getId());
            resp.setWorkDate(da.getWorkDate().toString());
            resp.setFacilityId(da.getFacilityId() == null ? null : da.getFacilityId().toString());
            resp.setStartTime(da.getStartTime().toString());
            resp.setEndTime(da.getEndTime().toString());
            resp.setSpecialityCode(da.getSpecialityCode());
            resp.setRoomAssignmentStatus(da.getRoomAssignmentStatus());
            resp.setRoomAssignedId(da.getRoomAssignedId() == null ? null : da.getRoomAssignedId().toString());
            // attach facility id so clients can reliably filter by facility
            resp.setFacilityId(da.getFacilityId() == null ? null : da.getFacilityId().toString());
            resp.setBookable(Boolean.TRUE.equals(da.getIsBookable()));
            // attach facility display info
            try {
                Facility fac = facilityRepo.findById(da.getFacilityId()).orElse(null);
                if (fac != null) {
                    resp.setFacilityName(fac.getName());
                    resp.setFacilityAddress(fac.getAddress());
                    resp.setFacilityState(fac.getState() == null ? null : fac.getState().getStateCode());
                }
            } catch (Exception ex) {
                // ignore
            }
            try {
                com.wecureit.dto.response.FacilityAvailabilityResponse far = doctorFacilityService.getFacilityAvailability(da.getFacilityId(), da.getWorkDate(), da.getStartTime(), da.getEndTime());
                if (far != null) {
                    resp.setRoomsTotal(far.roomsTotal);
                    resp.setOccupiedRooms(far.occupiedRooms);
                    resp.setAvailableRooms(far.availableRooms);
                }
            } catch (Exception ex) {
                // best-effort: if availability calculation fails, leave counts null
            }
            results.add(resp);
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> listAvailabilities(UUID doctorId, java.time.LocalDate from, java.time.LocalDate to) {
        List<com.wecureit.entity.DoctorAvailability> rows = availabilityRepo.findByDoctorIdAndWorkDateBetween(doctorId, from, to);
        List<AvailabilityResponse> out = new ArrayList<>();
        for (com.wecureit.entity.DoctorAvailability da : rows) {
            AvailabilityResponse resp = new AvailabilityResponse();
            resp.setId(da.getId());
            resp.setWorkDate(da.getWorkDate().toString());
            resp.setStartTime(da.getStartTime().toString());
            resp.setEndTime(da.getEndTime().toString());
            resp.setSpecialityCode(da.getSpecialityCode());
            resp.setRoomAssignmentStatus(da.getRoomAssignmentStatus());
            resp.setRoomAssignedId(da.getRoomAssignedId() == null ? null : da.getRoomAssignedId().toString());
            resp.setBookable(Boolean.TRUE.equals(da.getIsBookable()));
            resp.setAllowWalkIn(da.getAllowWalkIn());
            try {
                com.wecureit.dto.response.FacilityAvailabilityResponse far = doctorFacilityService.getFacilityAvailability(da.getFacilityId(), da.getWorkDate(), da.getStartTime(), da.getEndTime());
                if (far != null) {
                    resp.setRoomsTotal(far.roomsTotal);
                    resp.setOccupiedRooms(far.occupiedRooms);
                    resp.setAvailableRooms(far.availableRooms);
                }
            } catch (Exception ex) {
                // ignore
            }
            // attach facility display info for list responses as well
            try {
                Facility fac = facilityRepo.findById(da.getFacilityId()).orElse(null);
                if (fac != null) {
                    resp.setFacilityName(fac.getName());
                    resp.setFacilityAddress(fac.getAddress());
                    resp.setFacilityState(fac.getState() == null ? null : fac.getState().getStateCode());
                }
            } catch (Exception ex) {
                // ignore
            }
            // ensure allowWalkIn also provided
            resp.setAllowWalkIn(da.getAllowWalkIn());
            out.add(resp);
        }
        return out;
    }

    @Transactional
    public boolean assignRoom(UUID doctorId, UUID availabilityId, UUID roomId) {
        DoctorAvailability da = availabilityRepo.findById(availabilityId).orElse(null);
        if (da == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Availability not found");
        if (!da.getDoctorId().equals(doctorId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not allowed");

        LocalDateTime startAt = LocalDateTime.of(da.getWorkDate(), da.getStartTime());
        LocalDateTime endAt = LocalDateTime.of(da.getWorkDate(), da.getEndTime());

        // try to reserve the room
        try {
            RoomSchedule rs = new RoomSchedule();
            rs.setRoomId(roomId);
            rs.setDoctorId(doctorId);
            rs.setStartAt(startAt);
            rs.setEndAt(endAt);
            rs.setPurpose("AVAILABILITY_ASSIGN");
            roomScheduleRepo.save(rs);
        } catch (DataIntegrityViolationException ex) {
            // conflict
            return false;
        }

        da.setRoomAssignedId(roomId);
        da.setRoomAssignmentStatus("ASSIGNED");
        da.setIsBookable(true);
        availabilityRepo.save(da);
        return true;
    }

    @Transactional
    public DoctorAvailability setAllowWalkIn(UUID doctorId, UUID availabilityId, boolean allow) {
        DoctorAvailability da = availabilityRepo.findById(availabilityId).orElse(null);
        if (da == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Availability not found");
        if (!da.getDoctorId().equals(doctorId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not allowed");

        da.setAllowWalkIn(allow);
        if (allow) {
            da.setIsBookable(false);
            da.setRoomAssignmentStatus("NONE");
        }
        availabilityRepo.save(da);
        return da;
    }

    @Transactional
    public void deleteAvailability(UUID doctorId, UUID availabilityId) {
        com.wecureit.entity.DoctorAvailability da = availabilityRepo.findById(availabilityId).orElse(null);
        if (da == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Availability not found");
        if (!da.getDoctorId().equals(doctorId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not allowed");

        // If a room was assigned for this availability (AVAILABILITY_ASSIGN), remove the room_schedule placeholder
        if (da.getRoomAssignedId() != null) {
            java.time.LocalDateTime startAt = java.time.LocalDateTime.of(da.getWorkDate(), da.getStartTime());
            java.time.LocalDateTime endAt = java.time.LocalDateTime.of(da.getWorkDate(), da.getEndTime());
            try {
                java.util.List<com.wecureit.entity.RoomSchedule> overlaps = roomScheduleRepo.findOverlapping(da.getRoomAssignedId(), startAt, endAt);
                if (overlaps != null) {
                    for (com.wecureit.entity.RoomSchedule rs : overlaps) {
                        if (java.util.Objects.equals(rs.getDoctorId(), doctorId) && "AVAILABILITY_ASSIGN".equals(rs.getPurpose()) && rs.getAppointmentId() == null) {
                            roomScheduleRepo.delete(rs);
                        }
                    }
                }
            } catch (Exception ex) {
                // best-effort: ignore failures to remove schedule, proceed to delete availability
            }
        }

        availabilityRepo.delete(da);
    }

}
