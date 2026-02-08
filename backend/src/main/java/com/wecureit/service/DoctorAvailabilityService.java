package com.wecureit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.request.AvailabilityRequest;
import com.wecureit.dto.response.AvailabilityResponse;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.Facility;
import com.wecureit.entity.RoomSchedule;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.RoomRepository;
import com.wecureit.repository.RoomScheduleRepository;

@Service
public class DoctorAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(DoctorAvailabilityService.class);

    @Autowired
    private DoctorAvailabilityRepository availabilityRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private RoomScheduleService roomScheduleService;

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
            try {
                availabilityRepo.save(da);
            } catch (DataIntegrityViolationException dive) {
                // Handle DB unique constraint/race conditions gracefully and map to HTTP 409
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Conflict: uniqueness constraint violated");
            }

            // Do NOT attempt to reserve rooms when saving availability.
            // Availabilities are allowed to overlap; room reservations are created
            // only when an appointment is booked. By default we keep the
            // availability bookable so patients can book — the actual room
            // assignment will happen during appointment creation. If there are
            // genuinely no rooms available at booking time, the system will
            // fall back to marking the slot as Walk-in during that flow.
            da.setRoomAssignmentStatus("NONE");
            da.setIsBookable(true);
            try {
                availabilityRepo.save(da);
            } catch (DataIntegrityViolationException dive) {
                // Defensive second save as well (roomAssignmentStatus update) — treat conflicts as 409
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Conflict: uniqueness constraint violated");
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
            // populate occupied appointment ranges for this availability window
            try {
                java.util.List<String> occ = getOccupiedAppointmentsForDoctor(da.getDoctorId(), da.getWorkDate(), da.getStartTime(), da.getEndTime());
                resp.setOccupiedAppointments(occ);
            } catch (Exception ex) {
                // ignore
            }
            out.add(resp);
        }
        return out;
    }

    // helper to collect appointment ranges for a given availability window and doctor
    @Autowired
    private com.wecureit.repository.AppointmentRepository appointmentRepo;

    private java.util.List<String> getOccupiedAppointmentsForDoctor(UUID doctorId, LocalDate workDate, LocalTime startTime, LocalTime endTime) {
        java.util.List<String> out = new java.util.ArrayList<>();
        try {
            LocalDateTime startAt = LocalDateTime.of(workDate, startTime);
            LocalDateTime endAt = LocalDateTime.of(workDate, endTime);
            java.util.List<com.wecureit.entity.Appointment> appts = appointmentRepo.findAppointmentsForDoctor(doctorId, startAt, endAt);
            if (appts != null) {
                for (com.wecureit.entity.Appointment a : appts) {
                    // ignore inactive/cancelled appointments so cancelled slots become available immediately
                    if (a.getIsActive() != null && Boolean.FALSE.equals(a.getIsActive())) continue;
                    if (a.getStartTime() != null && a.getEndTime() != null) {
                        String s = a.getStartTime().toLocalTime().toString();
                        String e = a.getEndTime().toLocalTime().toString();
                        out.add(s + "|" + e);
                    }
                }
            }
        } catch (Exception ex) {
            // best-effort: ignore
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
    log.debug("AssignRoom computed startAt={} endAt={} zone={}", startAt, endAt, java.time.ZoneId.systemDefault());

        // try to reserve the room
        RoomSchedule rs = new RoomSchedule();
        rs.setRoomId(roomId);
        rs.setDoctorId(doctorId);
        rs.setStartAt(startAt);
        rs.setEndAt(endAt);
        rs.setPurpose("AVAILABILITY_ASSIGN");
        RoomSchedule saved = roomScheduleService.tryReserve(rs);
        if (saved == null) return false;

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
            log.debug("DeleteAvailability computed startAt={} endAt={} zone={}", startAt, endAt, java.time.ZoneId.systemDefault());
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
