package com.wecureit.service;

import java.time.LocalDate;
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
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorFacilityLockRepository;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.FacilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DoctorAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorAvailabilityService.class);

    @Autowired
    private DoctorAvailabilityRepository availabilityRepo;

    @Autowired
    private FacilityRepository facilityRepo;

    @Autowired
    private DoctorLicenseRepository doctorLicenseRepo;

    @Autowired
    private DoctorFacilityService doctorFacilityService;

    @Autowired
    private DoctorFacilityLockRepository doctorFacilityLockRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Transactional
    public List<AvailabilityResponse> saveAvailabilities(UUID doctorId, List<AvailabilityRequest> items) {
        List<AvailabilityResponse> results = new ArrayList<>();

        for (AvailabilityRequest it : items) {
            // parse incoming workDate as local date (accept either YYYY-MM-DD or ISO datetime)
            LocalDate workDate = null;
            if (it.getWorkDate() != null && !it.getWorkDate().isBlank()) {
                String raw = it.getWorkDate();
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

            // enforce facility lock: if the doctor is locked to a facility for this date, only allow availabilities for that facility
            var lockOpt = doctorFacilityLockRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
            if (lockOpt.isPresent()) {
                var lock = lockOpt.get();
                if (!lock.getFacilityId().equals(facId)) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Doctor is locked to a different facility for this date");
                }
            }

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
            try {
                availabilityRepo.save(da);
            } catch (DataIntegrityViolationException dive) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Conflict: uniqueness constraint violated");
            }

            AvailabilityResponse resp = new AvailabilityResponse();
            resp.setId(da.getId());
            resp.setWorkDate(da.getWorkDate().toString());
            resp.setFacilityId(da.getFacilityId() == null ? null : da.getFacilityId().toString());
            resp.setStartTime(da.getStartTime().toString());
            resp.setEndTime(da.getEndTime().toString());
            resp.setSpecialityCode(da.getSpecialityCode());
            // booking flag is not persisted server-side anymore; treat availability as bookable by default
            resp.setBookable(true);
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

    public List<AvailabilityResponse> listAvailabilities(UUID doctorId, LocalDate from, LocalDate to) {
        List<com.wecureit.entity.DoctorAvailability> avails = availabilityRepo.findByDoctorIdAndWorkDateBetween(doctorId, from, to);
        try {
            int found = (avails == null ? 0 : avails.size());
            logger.info("listAvailabilities - doctorId={} from={} to={} -> found={}", doctorId, from, to, found);
            if (avails != null && !avails.isEmpty()) {
                String ids = avails.stream().map(a -> a.getId() == null ? "null" : a.getId().toString()).reduce((x,y) -> x + "," + y).orElse("");
                logger.debug("listAvailabilities - availIds=[{}]", ids);
            }
        } catch (Exception e) {
            logger.warn("listAvailabilities - logging failed: {}", e.getMessage());
        }
        List<AvailabilityResponse> out = new ArrayList<>();
        if (avails == null) return out;
        for (com.wecureit.entity.DoctorAvailability da : avails) {
            // Only include active availabilities. The 'is_active' column is used to hide availabilities
            // when a doctor is locked to a facility for the day.
            if (da.getIsActive() != null && !da.getIsActive()) continue;
            AvailabilityResponse resp = new AvailabilityResponse();
            resp.setId(da.getId());
            resp.setWorkDate(da.getWorkDate() == null ? null : da.getWorkDate().toString());
            resp.setFacilityId(da.getFacilityId() == null ? null : da.getFacilityId().toString());
            resp.setStartTime(da.getStartTime() == null ? null : da.getStartTime().toString());
            resp.setEndTime(da.getEndTime() == null ? null : da.getEndTime().toString());
            resp.setSpecialityCode(da.getSpecialityCode());
            // booking flag not stored; default to true for display
            resp.setBookable(true);
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
            out.add(resp);
        }
        return out;
    }

    public void deleteAvailability(UUID doctorId, UUID availabilityId) {
        // simple ownership guard: ensure the availability belongs to the doctor
        com.wecureit.entity.DoctorAvailability da = availabilityRepo.findById(availabilityId).orElseThrow(() -> new IllegalArgumentException("Availability not found"));
        if (da.getDoctorId() == null || !da.getDoctorId().equals(doctorId)) throw new IllegalArgumentException("Availability does not belong to doctor");
        // If a facility lock exists for this doctor on the availability's date and there are active appointments for that facility, disallow deletion
        var lockOpt = doctorFacilityLockRepository.findByDoctorIdAndWorkDate(doctorId, da.getWorkDate());
        if (lockOpt.isPresent()) {
            var lock = lockOpt.get();
            if (!lock.getFacilityId().equals(da.getFacilityId())) {
                throw new IllegalArgumentException("Cannot delete availability: doctor is locked to a different facility for this date");
            }
            // check for any active appointments for this doctor on the day at this facility
            java.time.LocalDateTime startOfDay = da.getWorkDate().atStartOfDay();
            java.time.LocalDateTime endOfDay = da.getWorkDate().plusDays(1).atStartOfDay();
            var appts = appointmentRepository.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
            if (appts != null) {
                for (var a : appts) {
                    if ((a.getIsActive() == null || a.getIsActive()) && a.getFacility() != null && a.getFacility().getId() != null && a.getFacility().getId().equals(da.getFacilityId())) {
                        throw new IllegalArgumentException("Cannot delete availability: there are active appointments for this doctor at the facility on this date");
                    }
                }
            }
        }

        availabilityRepo.delete(da);
    }

}
