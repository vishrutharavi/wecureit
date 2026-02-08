package com.wecureit.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDate;
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
import com.wecureit.repository.AppointmentHistoryRepository;
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
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final com.wecureit.repository.RoomRepository roomRepository;
    private final RoomScheduleService roomScheduleService;

    public AppointmentService(AppointmentRepository repo,
                              PatientRepository patientRepository,
                              DoctorAvailabilityRepository doctorAvailabilityRepository,
                              FacilityRepository facilityRepository,
                              SpecialityRepository specialityRepository,
                              RoomScheduleRepository roomScheduleRepository,
                              com.wecureit.repository.RoomRepository roomRepository,
                              AppointmentHistoryRepository appointmentHistoryRepository,
                              RoomScheduleService roomScheduleService) {
        this.repo = repo;
        this.patientRepository = patientRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
        this.roomScheduleRepository = roomScheduleRepository;
        this.roomRepository = roomRepository;
        this.appointmentHistoryRepository = appointmentHistoryRepository;
        this.roomScheduleService = roomScheduleService;
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, String cancelledBy) {
        try {
            Appointment appt = repo.findById(appointmentId).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            System.out.println("cancelAppointment: found appointment id=" + appointmentId + " patientId=" + (appt.getPatient() == null ? null : appt.getPatient().getId()));

            // mark inactive
            appt.setIsActive(Boolean.FALSE);
            appt = repo.save(appt);
            System.out.println("cancelAppointment: appointment marked inactive and saved id=" + appt.getId());

            // persist history record
                try {
                    com.wecureit.entity.AppointmentHistory hist = new com.wecureit.entity.AppointmentHistory();
                    // Let JPA/hibernate generate the UUID id. Setting the id manually can force a merge
                    // and lead to "stale object" / optimistic locking problems if an id collides or
                    // the persistence provider expects to manage id generation. Do not pre-set the id here.
                    // Note: during DB migrations the appointment_history.appointment_id column
                    // may still be bigint which will cause SQL/type errors. To be resilient
                    // (so cancellation can proceed) we skip setting appointmentId here and
                    // instead record patient/doctor/cancelledBy. Once DB is migrated we can
                    // restore linking the history to the appointment UUID.
                    if (appt.getDoctorAvailability() != null) hist.setDoctorAvailabilityId(appt.getDoctorAvailability().getId());
                    if (appt.getPatient() != null) hist.setPatientId(appt.getPatient().getId());
                    // link history to the appointment UUID now that DB column is UUID
                    try {
                        if (appt.getUuid() != null) hist.setAppointmentId(appt.getUuid());
                    } catch (Exception e) {
                        // If for any reason setting appointmentId fails (unlikely now), continue without linking
                        System.out.println("cancelAppointment: unable to set appointmentId on history: " + e.getMessage());
                    }
                    // normalize cancelledBy to canonical values
                    String who = (cancelledBy == null) ? "unknown" : cancelledBy.trim().toLowerCase();
                    if (who.startsWith("pat")) who = "patient";
                    else if (who.startsWith("doc")) who = "doctor";
                    else if (who.startsWith("admin")) who = "admin";
                    hist.setCancelledBy(who);
                    System.out.println("cancelAppointment: saving history record for appointment=" + appt.getId() + " cancelledBy=" + who);
                    // saveAndFlush so we get immediate INSERT semantics and any DB constraint errors
                    // surface inside this transaction block (helps debugging and avoids later merges)
                    appointmentHistoryRepository.saveAndFlush(hist);
                    System.out.println("cancelAppointment: history saved id=" + hist.getId());
                } catch (Exception ex) {
                    // If history save fails due to a schema/type mismatch (common during migrations),
                    // log the error and attempt a best-effort fallback so the main cancellation can succeed.
                    System.out.println("cancelAppointment: error saving history: " + ex.getMessage());
                    Throwable root = ex.getCause();
                    if (root != null) System.out.println("cancelAppointment: history save root cause: " + root.getClass().getName() + " -> " + root.getMessage());
                    ex.printStackTrace();
                    try {
                        // Best-effort fallback: save history without appointment reference (appointment_id NULL)
                        com.wecureit.entity.AppointmentHistory hist2 = new com.wecureit.entity.AppointmentHistory();
                        // leave appointmentId null in fallback to avoid schema type mismatch when setting fails
                        // hist2.setAppointmentId(null);
                        if (appt.getDoctorAvailability() != null) hist2.setDoctorAvailabilityId(appt.getDoctorAvailability().getId());
                        if (appt.getPatient() != null) hist2.setPatientId(appt.getPatient().getId());
                        String who = (cancelledBy == null) ? "unknown" : cancelledBy.trim().toLowerCase();
                        if (who.startsWith("pat")) who = "patient";
                        else if (who.startsWith("doc")) who = "doctor";
                        else if (who.startsWith("admin")) who = "admin";
                        hist2.setCancelledBy(who);
                        appointmentHistoryRepository.saveAndFlush(hist2);
                        System.out.println("cancelAppointment: fallback history saved id=" + hist2.getId());
                    } catch (Exception ex2) {
                        System.out.println("cancelAppointment: fallback history save also failed: " + ex2.getMessage());
                        ex2.printStackTrace();
                        // Do NOT rethrow - we want cancellation to succeed even if history logging fails during migration
                    }
                }

            // free any room_schedule entries tied to this appointment
            // NOTE: there is a known schema mismatch risk where the room_schedule.appointment_id
            // column in the database may be a UUID while the application expects a bigint/Long.
            // That will surface as a SQLGrammarException / PSQLException (operator does not exist: uuid = bigint).
            // To avoid the whole cancel flow failing (and the transaction rolling back) while we migrate the DB,
            // catch and log errors here. This preserves cancellation for the appointment record while surfacing
            // the exact DB error for follow-up migration work.
            try {
                java.util.List<RoomSchedule> schedules = roomScheduleRepository.findByAppointmentId(appt.getUuid());
                System.out.println("cancelAppointment: found " + (schedules == null ? 0 : schedules.size()) + " room schedules to free");
                if (schedules != null) {
                    for (RoomSchedule rs : schedules) {
                        System.out.println("cancelAppointment: freeing roomSchedule id=" + rs.getId());
                        rs.setAppointmentId(null);
                        rs.setPurpose("AVAILABILITY_ASSIGN");
                        roomScheduleRepository.save(rs);
                    }
                }
            } catch (org.springframework.dao.InvalidDataAccessResourceUsageException sqlEx) {
                // specific handling/logging for SQL grammar/type mismatch (should be unlikely now)
                System.out.println("cancelAppointment: SQL error when freeing room schedules: " + sqlEx.getMessage());
                Throwable root = sqlEx.getCause();
                if (root != null) {
                    System.out.println("cancelAppointment: root cause: " + root.getClass().getName() + " -> " + root.getMessage());
                }
                sqlEx.printStackTrace();
                // Do NOT rethrow here to avoid rolling back the whole cancellation —
                // room_schedule entries will need a separate migration/repair.
            } catch (Exception ex) {
                // generic fallback: log and continue so cancellation can succeed
                System.out.println("cancelAppointment: error freeing room schedules: " + ex.getMessage());
                ex.printStackTrace();
            }

            // After cancelling this appointment, recompute break assignments for the affected doctor/day
            try {
                if (appt.getDoctorAvailability() != null && appt.getStartTime() != null) {
                    LocalDate day = appt.getStartTime().toLocalDate();
                    recomputeBreaksForDoctorAndDay(appt.getDoctorAvailability().getDoctorId(), day);
                }
            } catch (Exception ex) {
                System.out.println("cancelAppointment: error recomputing breaks: " + ex.getMessage());
                ex.printStackTrace();
            }

            return appt;
        } catch (Exception e) {
            // Log and rethrow to make rollback cause visible in logs
            System.out.println("cancelAppointment: fatal error, will rethrow: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public Appointment createAppointment(AppointmentRequest req) {
        // prevent duplicate appointment for same patient at the same start time
        if (req.getPatientId() != null && req.getStartTime() != null) {
            // parse startTime if it's LocalDateTime already in DTO - in DTO it's LocalDateTime
            LocalDateTime start = req.getStartTime();
            // allow recreation if a previous appointment at this time exists but is inactive (cancelled)
            try {
                    var existingOpt = repo.findByPatientIdAndStartTime(req.getPatientId(), start);
                    if (existingOpt.isPresent()) {
                        var existing = existingOpt.get();
                        // Treat only Boolean.TRUE as active. NULL will be treated as inactive so cancelled rows
                        // (or rows created during migrations) don't incorrectly block new appointments.
                        if (Boolean.TRUE.equals(existing.getIsActive())) {
                            throw new IllegalArgumentException("Appointment already exists for this patient at the selected time");
                        }
                        // otherwise the existing appointment is inactive/cancelled and we allow creating a new one
                    }
            } catch (Exception ex) {
                // If repository method fails for any reason, fall back to conservative exists check
                // use active-only existence check in fallback to avoid treating inactive rows as blocking
                if (repo.existsByPatientIdAndStartTimeAndIsActiveTrue(req.getPatientId(), start)) {
                    throw new IllegalArgumentException("Appointment already exists for this patient at the selected time");
                }
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

    // ensure appointment UUID is set for new rows (DB column 'uuid')
    if (appt.getUuid() == null) appt.setUuid(UUID.randomUUID());

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
                        rs.setAppointmentId(saved.getUuid());
                        rs.setStartAt(saved.getStartTime());
                        rs.setEndAt(saved.getEndTime());
                        rs.setPurpose("APPOINTMENT");
                        RoomSchedule savedRs = roomScheduleService.tryReserve(rs);
                        if (savedRs != null) {
                            saved.setRoomSchedule(savedRs);
                            saved = repo.save(saved);
                            break;
                        }
                        break;
                    }
                }
            } catch (Exception ex) {
                // best-effort: if room assignment fails, leave appointment without a room
            }
        }

        // After creating the appointment, recompute any break assignments for the doctor/day
        try {
            if (saved.getDoctorAvailability() != null && saved.getStartTime() != null) {
                LocalDate day = saved.getStartTime().toLocalDate();
                recomputeBreaksForDoctorAndDay(saved.getDoctorAvailability().getDoctorId(), day);
            }
        } catch (Exception ex) {
            // log and continue - break recompute is best-effort and should not fail the create
            System.out.println("createAppointment: error recomputing breaks: " + ex.getMessage());
            ex.printStackTrace();
        }

        return saved;
    }

    /**
     * Recomputes break assignments for all appointments of a doctor on a given day.
     * This is a best-effort recompute: clears existing break markers and then assigns
     * a 15-minute break at the end of the appointment that causes a contiguous work
     * block to exceed 60 minutes.
     */
    private void recomputeBreaksForDoctorAndDay(UUID doctorId, LocalDate day) {
        if (doctorId == null || day == null) return;
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();

        List<Appointment> appts = repo.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
        if (appts == null || appts.isEmpty()) return;

        // Only consider active appointments
        List<Appointment> active = new ArrayList<>();
        for (Appointment a : appts) {
            if (a.getIsActive() == null || a.getIsActive()) active.add(a);
        }
        if (active.isEmpty()) return;

        // sort by startTime
        active.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        // clear existing break markers
        for (Appointment a : active) {
            boolean changed = false;
            if (a.getBreakStartTime() != null) { a.setBreakStartTime(null); changed = true; }
            if (a.getBreakDurationMinutes() != null) { a.setBreakDurationMinutes(null); changed = true; }
            if (a.getBreakEndTime() != null) { a.setBreakEndTime(null); changed = true; }
            if (changed) repo.save(a);
        }

        // scan contiguous groups (gap <= 15 minutes considered continuous)
        final long BREAK_MINUTES = 15L;
        final long MAX_CONTINUOUS_MINUTES = 60L;
        long cumMinutes = 0L;
        LocalDateTime prevEnd = null;
        boolean groupExceeded = false;
        for (Appointment a : active) {
            // Prefer explicit duration if present and positive; otherwise compute from start/end timestamps.
            long dur = 0L;
            if (a.getDuration() != null && a.getDuration().longValue() > 0L) {
                dur = a.getDuration().longValue();
            } else if (a.getStartTime() != null && a.getEndTime() != null) {
                try {
                    dur = Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
                } catch (Exception ex) {
                    dur = 0L;
                }
            }
            if (prevEnd == null) {
                // start new group
                cumMinutes = dur;
                groupExceeded = false;
            } else {
                long gap = Duration.between(prevEnd, a.getStartTime()).toMinutes();
                if (gap < BREAK_MINUTES) {
                    // continuous
                    cumMinutes += dur;
                } else {
                    // gap large enough to consider a break already present; start new group
                    cumMinutes = dur;
                    groupExceeded = false;
                }
            }

            // if this appointment causes exceedance and we haven't already assigned a break in this group
            // Assign a break when cumulative minutes are strictly greater than the max continuous limit.
            // The policy requires a 15-minute break only when continuous work would be MORE than 60 minutes;
            // reaching exactly 60 minutes is allowed.
            if (!groupExceeded && cumMinutes > MAX_CONTINUOUS_MINUTES) {
                // assign break starting at appointment end time
                LocalDateTime bs = a.getEndTime();
                LocalDateTime be = bs.plusMinutes(BREAK_MINUTES);
                a.setBreakStartTime(bs);
                a.setBreakEndTime(be);
                a.setBreakDurationMinutes((int) BREAK_MINUTES);
                // persist immediately and log so we can trace why breaks may not be appearing in DB
                System.out.println("recomputeBreaksForDoctorAndDay: assigning break for appointment id=" + a.getId() + " breakStart=" + bs + " breakEnd=" + be);
                repo.saveAndFlush(a);
                groupExceeded = true;
            }

            prevEnd = a.getEndTime();
        }
    }

    public Appointment findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
    }

    /**
     * Maintenance helper: attempt to associate existing appointments (for a doctor on a day)
     * with a matching DoctorAvailability row when missing, then recompute breaks.
     * If associateMissing is false, only recompute breaks.
     */
    @Transactional
    public void recomputeBreaksAndAssociateIfNeeded(UUID doctorId, LocalDate day, boolean associateMissing) {
        if (doctorId == null || day == null) return;

        // optionally associate missing doctor_availability_id for appointments
        if (associateMissing) {
            try {
                // find all availabilities for this doctor on the day
                List<DoctorAvailability> avails = doctorAvailabilityRepository.findByDoctorIdAndWorkDateBetween(doctorId, day, day);
                // find appointments for doctor/day
                LocalDateTime startOfDay = day.atStartOfDay();
                LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
                List<Appointment> appts = repo.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
                if (appts != null) {
                    for (Appointment a : appts) {
                        if (a.getDoctorAvailability() == null && a.getStartTime() != null && a.getEndTime() != null) {
                            // try to find an availability window that fully contains the appointment
                            for (DoctorAvailability da : avails) {
                                java.time.LocalDateTime availStart = java.time.LocalDateTime.of(da.getWorkDate(), da.getStartTime());
                                java.time.LocalDateTime availEnd = java.time.LocalDateTime.of(da.getWorkDate(), da.getEndTime());
                                if (!a.getStartTime().isBefore(availStart) && !a.getEndTime().isAfter(availEnd)) {
                                    a.setDoctorAvailability(da);
                                    repo.save(a);
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("recomputeBreaksAndAssociateIfNeeded: error during association: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // now call the existing recompute logic to set/clear break fields
        recomputeBreaksForDoctorAndDay(doctorId, day);
    }

    public List<Appointment> findByPatientId(UUID patientId) {
        var list = repo.findByPatientIdOrderByStartTimeDesc(patientId);
        if (list == null) return new ArrayList<>();
        // return only active appointments by default
        var out = new ArrayList<Appointment>();
        for (Appointment a : list) {
            if (a.getIsActive() == null || a.getIsActive().booleanValue()) out.add(a);
        }
        return out;
    }
}
