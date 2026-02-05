package com.wecureit.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.request.AppointmentRequest;
import com.wecureit.entity.Appointment;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Patient;
import com.wecureit.entity.RoomSchedule;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.PatientRepository;
import com.wecureit.repository.RoomScheduleRepository;
import com.wecureit.repository.SpecialityRepository;

@Service
public class AppointmentService {

    private final AppointmentRepository repo;
    private final PatientRepository patientRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;
    private final RoomScheduleRepository roomScheduleRepository;
    private final com.wecureit.repository.RoomRepository roomRepository;

    public AppointmentService(AppointmentRepository repo,
                              PatientRepository patientRepository,
                              DoctorAvailabilityRepository doctorAvailabilityRepository,
                              FacilityRepository facilityRepository,
                              SpecialityRepository specialityRepository,
                              RoomScheduleRepository roomScheduleRepository,
                              com.wecureit.repository.RoomRepository roomRepository) {
        this.repo = repo;
        this.patientRepository = patientRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
        this.roomScheduleRepository = roomScheduleRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Appointment createAppointment(AppointmentRequest req) {
        // prevent duplicate appointment for same patient at the same start time
        if (req.getPatientId() != null && req.getStartTime() != null) {
            // parse startTime if it's LocalDateTime already in DTO - in DTO it's LocalDateTime
            LocalDateTime start = req.getStartTime();
            if (repo.existsByPatientIdAndStartTime(req.getPatientId(), start)) {
                throw new IllegalArgumentException("Appointment already exists for this patient at the selected time");
            }
        }

    Appointment appt = new Appointment();
        appt.setDate(req.getDate());
        appt.setDuration(req.getDuration());
        appt.setStartTime(req.getStartTime());
        appt.setEndTime(req.getEndTime());
        appt.setChiefComplaints(req.getChiefComplaints());
        appt.setIsActive(req.getIsActive() == null ? Boolean.TRUE : req.getIsActive());

        if (req.getPatientId() != null) {
            Patient p = patientRepository.findById(req.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
            appt.setPatient(p);
        }

        if (req.getDoctorAvailabilityId() != null) {
            DoctorAvailability da = doctorAvailabilityRepository.findById(req.getDoctorAvailabilityId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor availability not found"));
            appt.setDoctorAvailability(da);
        }

        if (req.getFacilityId() != null) {
            Facility f = facilityRepository.findById(req.getFacilityId())
                    .orElseThrow(() -> new IllegalArgumentException("Facility not found"));
            appt.setFacility(f);
        }

        if (req.getSpecialityId() != null && !req.getSpecialityId().isBlank()) {
            Speciality s = specialityRepository.findById(req.getSpecialityId())
                    .orElseThrow(() -> new IllegalArgumentException("Speciality not found"));
            appt.setSpeciality(s);
        }

        if (req.getRoomScheduleId() != null) {
            RoomSchedule rs = roomScheduleRepository.findById(req.getRoomScheduleId())
                    .orElseThrow(() -> new IllegalArgumentException("Room schedule not found"));
            appt.setRoomSchedule(rs);
        }

        // save appointment first so we have an id to reference from RoomSchedule
        Appointment saved = repo.save(appt);

        // Auto-assign a room if none provided and we have facility + speciality
        if (saved.getRoomSchedule() == null && saved.getFacility() != null && saved.getSpeciality() != null && saved.getStartTime() != null && saved.getEndTime() != null) {
            try {
                java.util.List<com.wecureit.entity.Room> candidates = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(saved.getFacility().getId(), saved.getSpeciality().getSpecialityCode());
                for (com.wecureit.entity.Room r : candidates) {
                    // check overlapping schedules for this room
                    java.util.List<com.wecureit.entity.RoomSchedule> overlaps = roomScheduleRepository.findOverlapping(r.getId(), saved.getStartTime(), saved.getEndTime());
                    if (overlaps == null || overlaps.isEmpty()) {
                        RoomSchedule rs = new RoomSchedule();
                        rs.setRoomId(r.getId());
                        // if appointment has doctorAvailability set, use its doctorId
                        if (saved.getDoctorAvailability() != null) rs.setDoctorId(saved.getDoctorAvailability().getDoctorId());
                        rs.setAppointmentId(saved.getId());
                        rs.setStartAt(saved.getStartTime());
                        rs.setEndAt(saved.getEndTime());
                        rs.setPurpose("APPOINTMENT");
                        RoomSchedule savedRs = roomScheduleRepository.save(rs);
                        saved.setRoomSchedule(savedRs);
                        saved = repo.save(saved);
                        break;
                    }
                }
            } catch (Exception ex) {
                // best-effort: if room assignment fails, leave appointment without a room
            }
        }

        return saved;
    }

    public Appointment findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
    }

    public List<Appointment> findByPatientId(UUID patientId) {
        var list = repo.findByPatientIdOrderByStartTimeDesc(patientId);
        return list == null ? new ArrayList<>() : list;
    }
}
