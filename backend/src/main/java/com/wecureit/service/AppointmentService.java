package com.wecureit.service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import com.wecureit.dto.request.AppointmentRequest;
import com.wecureit.entity.Appointment;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Patient;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.AppointmentHistoryRepository;
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.PatientRepository;
import com.wecureit.repository.DoctorFacilityLockRepository;
// RoomSchedule/RoomScheduleRepository removed: room allocation/reservation is disabled
import com.wecureit.repository.SpecialityRepository;
// DoctorBreak entity removed; computed breaks use in-memory ComputedBreak class

@Service
public class AppointmentService {

    private final AppointmentRepository repo;
    private final PatientRepository patientRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final com.wecureit.repository.RoomRepository roomRepository;
    private final DoctorFacilityLockRepository doctorFacilityLockRepository;
    // doctorBreakRepository removed: breaks are computed on-the-fly but not persisted

    // ComputedBreak is a lightweight in-memory representation of a computed break
    // — used only for returning computed break info to callers. These are NOT persisted.
    public static class ComputedBreak {
        public UUID id;
        public LocalDateTime createdAt;
        public UUID doctorId;
        public LocalDate workDate;
        public UUID appointmentId;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public Boolean isActive;

        public ComputedBreak(UUID id, LocalDateTime createdAt, UUID doctorId, LocalDate workDate, UUID appointmentId, LocalDateTime startTime, LocalDateTime endTime, Boolean isActive) {
            this.id = id;
            this.createdAt = createdAt;
            this.doctorId = doctorId;
            this.workDate = workDate;
            this.appointmentId = appointmentId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isActive = isActive;
        }
    }

    public AppointmentService(AppointmentRepository repo,
                              PatientRepository patientRepository,
                              DoctorAvailabilityRepository doctorAvailabilityRepository,
                              FacilityRepository facilityRepository,
                              SpecialityRepository specialityRepository,
                              com.wecureit.repository.RoomRepository roomRepository,
                              AppointmentHistoryRepository appointmentHistoryRepository,
                              DoctorFacilityLockRepository doctorFacilityLockRepository,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.repo = repo;
        this.patientRepository = patientRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
    this.roomRepository = roomRepository;
    this.appointmentHistoryRepository = appointmentHistoryRepository;
    this.doctorFacilityLockRepository = doctorFacilityLockRepository;
    // doctorBreakRepository intentionally not assigned; breaks are compute-only now
        // set the publisher so this service can publish appointment change events
        this.applicationEventPublisher = applicationEventPublisher;
    }

    // injected by Spring to publish events when appointments change
    private org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public void setApplicationEventPublisher(org.springframework.context.ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
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
                    // set status column (schema was updated to include status)
                    hist.setStatus("cancelled");
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
                        hist2.setStatus("cancelled");
                        appointmentHistoryRepository.saveAndFlush(hist2);
                        System.out.println("cancelAppointment: fallback history saved id=" + hist2.getId());
                    } catch (Exception ex2) {
                        System.out.println("cancelAppointment: fallback history save also failed: " + ex2.getMessage());
                        ex2.printStackTrace();
                        // Do NOT rethrow - we want cancellation to succeed even if history logging fails during migration
                    }
                }

            // room_schedule entries are no longer managed by the appointment cancel flow

            // doctor_breaks table is not persisted anymore; skip deactivation step

            // Also run a synchronous recompute so breaks get updated immediately after cancellation
            try {
                if (appt.getDoctorAvailability() != null && appt.getStartTime() != null) {
                    LocalDate day = appt.getStartTime().toLocalDate();
                    try {
                        recomputeBreaksAndAssociateIfNeeded(appt.getDoctorAvailability().getDoctorId(), day, true);
                    } catch (Exception ex) {
                        System.out.println("cancelAppointment: synchronous recompute failed: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            } catch (Exception ex) {
                System.out.println("cancelAppointment: error preparing synchronous recompute: " + ex.getMessage());
            }

            // After cancelling this appointment, publish an event so an async listener recomputes breaks
            try {
                if (appt.getDoctorAvailability() != null && appt.getStartTime() != null) {
                    LocalDate day = appt.getStartTime().toLocalDate();
                    // publish async event (listener will call recompute)
                    try {
                        org.springframework.context.ApplicationEventPublisher publisher = this.applicationEventPublisher;
                        if (publisher != null) {
                            // AppointmentChangedEvent class removed; skip publishing event
                            System.out.println("cancelAppointment: skipping publishEvent (AppointmentChangedEvent not available)");
                        }
                    } catch (Exception ex) {
                        System.out.println("cancelAppointment: failed to publish AppointmentChangedEvent: " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                System.out.println("cancelAppointment: error preparing recompute event: " + ex.getMessage());
                ex.printStackTrace();
            }

            // After cancelling, try auto-unlock if there are no remaining active appointments for the doctor on that day
            try {
                if (appt.getDoctorAvailability() != null && appt.getStartTime() != null) {
                    LocalDate day = appt.getStartTime().toLocalDate();
                    tryAutoUnlockIfNoActiveAppointments(appt.getDoctorAvailability().getDoctorId(), day);
                }
            } catch (Exception ex) {
                System.out.println("cancelAppointment: auto-unlock check failed: " + ex.getMessage());
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

    // room assignment by room_schedule id is disabled; skip any provided roomScheduleId

    // ensure appointment UUID is set for new rows (DB column 'uuid')
    if (appt.getUuid() == null) appt.setUuid(UUID.randomUUID());

    // save appointment first so we have an id to reference from RoomSchedule
    Appointment saved = repo.save(appt);

        // room auto-assignment disabled: do not try to reserve rooms for appointments

        // After creating the appointment, publish an event so an async listener recomputes breaks
        try {
            if (saved.getDoctorAvailability() != null && saved.getStartTime() != null) {
                LocalDate day = saved.getStartTime().toLocalDate();
                // Ensure facility lock exists or matches the appointment's facility
                try {
                    if (saved.getDoctorAvailability() != null && saved.getFacility() != null) {
                        ensureDoctorFacilityLock(saved.getDoctorAvailability().getDoctorId(), day, saved.getFacility().getId());
                    }
                } catch (Exception ex) {
                    System.out.println("createAppointment: facility lock validation failed: " + ex.getMessage());
                    throw ex;
                }
                try {
                    org.springframework.context.ApplicationEventPublisher publisher = this.applicationEventPublisher;
                    if (publisher != null) {
                        // AppointmentChangedEvent class removed; skip publishing event
                        System.out.println("createAppointment: skipping publishEvent (AppointmentChangedEvent not available)");
                    }
                } catch (Exception ex) {
                    System.out.println("createAppointment: failed to publish AppointmentChangedEvent: " + ex.getMessage());
                }
                // Also invoke recompute synchronously so breaks are persisted immediately for booking flows
                try {
                    recomputeBreaksAndAssociateIfNeeded(saved.getDoctorAvailability().getDoctorId(), day, true);
                } catch (Exception ex) {
                    System.out.println("createAppointment: synchronous recompute failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            System.out.println("createAppointment: error preparing recompute event: " + ex.getMessage());
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
        // compute breaks but do NOT persist them (doctor_breaks table removed)
        if (doctorId == null || day == null) return;
    List<ComputedBreak> computedBreaks = computeBreaksForDoctorAndDay(doctorId, day);
        try {
            System.out.println("recomputeBreaksForDoctorAndDay: computed " + (computedBreaks == null ? 0 : computedBreaks.size()) + " break(s) for doctor=" + doctorId + " date=" + day + ". Not persisting as doctor_breaks table is disabled.");
        } catch (Exception ex) {
            System.out.println("recomputeBreaksForDoctorAndDay: error computing doctor_breaks: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Compute break assignments for appointments of a doctor on a given day, without persisting.
     */
    public List<ComputedBreak> getComputedBreaksForDoctorAndDay(UUID doctorId, LocalDate day) {
        return computeBreaksForDoctorAndDay(doctorId, day);
    }

    private List<ComputedBreak> computeBreaksForDoctorAndDay(UUID doctorId, LocalDate day) {
        List<ComputedBreak> computedBreaks = new ArrayList<>();
        if (doctorId == null || day == null) return computedBreaks;
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();

        List<Appointment> appts = repo.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
        if (appts == null || appts.isEmpty()) return computedBreaks;

        // Only consider active appointments
        List<Appointment> active = new ArrayList<>();
        for (Appointment a : appts) {
            if (a.getIsActive() == null || a.getIsActive()) active.add(a);
        }
        if (active.isEmpty()) return computedBreaks;

        // sort by startTime
        active.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        final long BREAK_MINUTES = 15L;
        final long MAX_CONTINUOUS_MINUTES = 60L;

        List<Appointment> group = new ArrayList<>();
        LocalDateTime groupEnd = null;
        LocalDateTime groupStart = null;

        java.util.function.BiFunction<List<Appointment>, Appointment, Long> computeUnionMinutes = (apptList, extra) -> {
            List<java.time.LocalDateTime[]> intervals = new ArrayList<>();
            for (Appointment aa : apptList) {
                if (aa.getStartTime() == null || aa.getEndTime() == null) continue;
                intervals.add(new java.time.LocalDateTime[] { aa.getStartTime(), aa.getEndTime() });
            }
            if (extra != null && extra.getStartTime() != null && extra.getEndTime() != null) {
                intervals.add(new java.time.LocalDateTime[] { extra.getStartTime(), extra.getEndTime() });
            }
            if (intervals.isEmpty()) return 0L;
            intervals.sort((i1, i2) -> i1[0].compareTo(i2[0]));
            long total = 0L;
            java.time.LocalDateTime curStart = intervals.get(0)[0];
            java.time.LocalDateTime curEnd = intervals.get(0)[1];
            for (int ii = 1; ii < intervals.size(); ii++) {
                java.time.LocalDateTime s = intervals.get(ii)[0];
                java.time.LocalDateTime e = intervals.get(ii)[1];
                if (!s.isAfter(curEnd) || Duration.between(curEnd, s).toMinutes() < BREAK_MINUTES) {
                    if (e.isAfter(curEnd)) curEnd = e;
                } else {
                    total += Duration.between(curStart, curEnd).toMinutes();
                    curStart = s;
                    curEnd = e;
                }
            }
            total += Duration.between(curStart, curEnd).toMinutes();
            return total;
        };

        for (Appointment a : active) {
            if (a.getStartTime() == null || a.getEndTime() == null) continue;
            if (group.isEmpty()) {
                group.add(a);
                groupStart = a.getStartTime();
                groupEnd = a.getEndTime();
                continue;
            }

            long gap = Duration.between(groupEnd, a.getStartTime()).toMinutes();
            if (gap < BREAK_MINUTES) {
                group.add(a);
                if (a.getEndTime().isAfter(groupEnd)) groupEnd = a.getEndTime();
                continue;
            }

            // process current group
            if (!group.isEmpty()) {
                boolean assigned = false;
                List<Appointment> prefix = new ArrayList<>();
                for (Appointment ga : group) {
                    prefix.add(ga);
                    long unionMins = computeUnionMinutes.apply(prefix, null);
                    if (unionMins > MAX_CONTINUOUS_MINUTES) {
                        LocalDateTime bs = ga.getEndTime();
                        LocalDateTime be = bs.plusMinutes(BREAK_MINUTES);
                        ComputedBreak cb = new ComputedBreak(UUID.randomUUID(), LocalDateTime.now(), doctorId, day, ga.getUuid(), bs, be, Boolean.TRUE);
                        computedBreaks.add(cb);
                        assigned = true;
                        break;
                    }
                }
            }

            group = new ArrayList<>();
            group.add(a);
            groupStart = a.getStartTime();
            groupEnd = a.getEndTime();
        }

        if (!group.isEmpty()) {
            List<Appointment> prefix = new ArrayList<>();
            for (Appointment ga : group) {
                prefix.add(ga);
                long unionMins = computeUnionMinutes.apply(prefix, null);
                if (unionMins > MAX_CONTINUOUS_MINUTES) {
                    LocalDateTime bs = ga.getEndTime();
                    LocalDateTime be = bs.plusMinutes(BREAK_MINUTES);
                    ComputedBreak cb = new ComputedBreak(UUID.randomUUID(), LocalDateTime.now(), doctorId, day, ga.getUuid(), bs, be, Boolean.TRUE);
                    computedBreaks.add(cb);
                    break;
                }
            }
        }

        return computedBreaks;
    }
    public Appointment findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
    }

    public java.util.Optional<Appointment> findByUuid(java.util.UUID uuid) {
        try {
            return repo.findByUuid(uuid);
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Return appointments for a doctor on a specific day (only active ones), sorted by startTime.
     */
    public List<Appointment> getAppointmentsForDoctorAndDay(UUID doctorId, LocalDate day) {
        if (doctorId == null || day == null) return new ArrayList<>();
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        List<Appointment> appts = repo.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
        if (appts == null) return new ArrayList<>();
        List<Appointment> active = new ArrayList<>();
        for (Appointment a : appts) {
            if (a.getIsActive() == null || a.getIsActive()) active.add(a);
        }
        active.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        return active;
    }

    /**
     * Return appointments for a doctor on a specific day including cancelled/inactive ones, sorted by startTime.
     */
    public List<Appointment> getAppointmentsForDoctorAndDayIncludeCancelled(UUID doctorId, LocalDate day) {
        if (doctorId == null || day == null) return new ArrayList<>();
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        List<Appointment> appts = repo.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
        if (appts == null) return new ArrayList<>();
        appts.sort((a, b) -> {
            if (a.getStartTime() == null) return -1;
            if (b.getStartTime() == null) return 1;
            return a.getStartTime().compareTo(b.getStartTime());
        });
        return appts;
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

    /**
     * Ensure a doctor_facility_lock exists for the given doctor+date and matches the provided facility.
     * If no lock exists, attempt to create one. If a lock exists for a different facility, throws IllegalArgumentException.
     * This method handles simple race via catching DataIntegrityViolationException and re-checking.
     */
    private void ensureDoctorFacilityLock(UUID doctorId, LocalDate day, UUID facilityId) {
        if (doctorId == null || day == null || facilityId == null) return;
        try {
            var existingOpt = doctorFacilityLockRepository.findByDoctorIdAndWorkDate(doctorId, day);
            if (existingOpt.isPresent()) {
                var lock = existingOpt.get();
                if (!facilityId.equals(lock.getFacilityId())) {
                    throw new IllegalArgumentException("Doctor is locked to a different facility for the day");
                }
                return;
            }

            // create a new lock row
            com.wecureit.entity.DoctorFacilityLock lock = new com.wecureit.entity.DoctorFacilityLock();
            lock.setDoctorId(doctorId);
            lock.setWorkDate(day);
            lock.setFacilityId(facilityId);
            try {
                doctorFacilityLockRepository.save(lock);
                // deactivate other availabilities for this doctor on the date (server-side enforcement)
                try {
                    doctorAvailabilityRepository.deactivateOtherAvailabilities(doctorId, day, facilityId);
                } catch (Exception e) {
                    System.out.println("ensureDoctorFacilityLock: failed to deactivate other availabilities: " + e.getMessage());
                }
            } catch (DataIntegrityViolationException dive) {
                // another transaction likely created the lock concurrently; re-check
                var after = doctorFacilityLockRepository.findByDoctorIdAndWorkDate(doctorId, day);
                if (after.isPresent()) {
                    if (!facilityId.equals(after.get().getFacilityId())) {
                        throw new IllegalArgumentException("Doctor is locked to a different facility for the day (concurrent)");
                    }
                    // ensure other availabilities are deactivated even if we lost the race
                    try {
                        doctorAvailabilityRepository.deactivateOtherAvailabilities(doctorId, day, facilityId);
                    } catch (Exception ex) {
                        System.out.println("ensureDoctorFacilityLock: failed to deactivate other availabilities after concurrent lock: " + ex.getMessage());
                    }
                    return;
                }
                // otherwise rethrow
                throw dive;
            }
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    /**
     * If there are no active appointments for the doctor on the given day, delete any existing lock.
     */
    private void tryAutoUnlockIfNoActiveAppointments(UUID doctorId, LocalDate day) {
        if (doctorId == null || day == null) return;
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        List<Appointment> appts = repo.findAppointmentsForDoctor(doctorId, startOfDay, endOfDay);
        boolean anyActive = false;
        if (appts != null) {
            for (Appointment a : appts) {
                if (a.getIsActive() == null || a.getIsActive()) { anyActive = true; break; }
            }
        }
        if (!anyActive) {
            try {
                doctorFacilityLockRepository.deleteByDoctorIdAndWorkDate(doctorId, day);
            } catch (Exception ex) {
                System.out.println("tryAutoUnlockIfNoActiveAppointments: failed to delete lock: " + ex.getMessage());
            }
        }
    }

    public List<Appointment> findByPatientId(UUID patientId) {
        var list = repo.findByPatientIdOrderByStartTimeDesc(patientId);
        if (list == null) return new ArrayList<>();
        // return only active appointments by default
        var out = new ArrayList<Appointment>();
        for (Appointment a : list) {
            boolean activeFlag = (a.getIsActive() == null) ? true : a.getIsActive().booleanValue();
            if (!activeFlag) continue;
            // Defensive: if there's a completed history row for this appointment, treat it as not upcoming
            try {
                if (a.getUuid() != null) {
                    var histories = appointmentHistoryRepository.findByAppointmentId(a.getUuid());
                    if (histories != null) {
                        boolean hasCompleted = false;
                        for (var h : histories) {
                            if (h == null) continue;
                            if (h.getStatus() != null && h.getStatus().equalsIgnoreCase("completed")) { hasCompleted = true; break; }
                            if (h.getStatus() != null && h.getStatus().equalsIgnoreCase("cancelled") && h.getCancelledBy() != null && h.getCancelledBy().equalsIgnoreCase("doctor")) { hasCompleted = true; break; }
                        }
                        if (hasCompleted) continue; // skip adding to upcoming
                    }
                }
            } catch (Exception ex) {
                // on error, fall back to DB's isActive flag
            }
            out.add(a);
        }
        return out;
    }

    /**
     * Mark an appointment as completed by the doctor and record a history row.
     * This does not change appointment.isActive (appointments remain as records).
     */
    @Transactional
    public Appointment markAppointmentCompleted(Long appointmentId, UUID doctorId) {
        Appointment appt = repo.findById(appointmentId).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // ensure the appointment belongs to the doctor calling this
        if (appt.getDoctorAvailability() == null || appt.getDoctorAvailability().getDoctorId() == null || !appt.getDoctorAvailability().getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("Appointment does not belong to the specified doctor");
        }

        // Do not allow completing appointments scheduled for future dates (tomorrow or later)
        try {
            if (appt.getStartTime() != null) {
                java.time.LocalDate apptDate = appt.getStartTime().toLocalDate();
                java.time.LocalDate today = java.time.LocalDate.now();
                if (apptDate.isAfter(today)) {
                    throw new IllegalArgumentException("Cannot mark future appointments as completed");
                }
            }
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception ex) {
            System.out.println("markAppointmentCompleted: date check failed: " + ex.getMessage());
        }

        try {
            com.wecureit.entity.AppointmentHistory hist = new com.wecureit.entity.AppointmentHistory();
            try { if (appt.getUuid() != null) hist.setAppointmentId(appt.getUuid()); } catch (Exception e) {}
            try { if (appt.getDoctorAvailability() != null) hist.setDoctorAvailabilityId(appt.getDoctorAvailability().getId()); } catch (Exception e) {}
            try { if (appt.getPatient() != null) hist.setPatientId(appt.getPatient().getId()); } catch (Exception e) {}
            // reuse cancelledBy field to record actor; normalize to 'doctor'
            hist.setCancelledBy("doctor");
            hist.setStatus("COMPLETED");
            appointmentHistoryRepository.saveAndFlush(hist);
        } catch (Exception ex) {
            System.out.println("markAppointmentCompleted: error saving history: " + ex.getMessage());
            ex.printStackTrace();
        }

        // mark the appointment inactive so it no longer appears in upcoming lists
        try {
            appt.setIsActive(Boolean.FALSE);
            appt = repo.save(appt);
        } catch (Exception ex) {
            System.out.println("markAppointmentCompleted: failed to mark appointment inactive: " + ex.getMessage());
            ex.printStackTrace();
        }

        return appt;
    }

    /**
     * Return appointment_history rows for a patient.
     */
    public java.util.List<com.wecureit.entity.AppointmentHistory> getAppointmentHistoryForPatient(UUID patientId) {
        if (patientId == null) return new java.util.ArrayList<>();
        try {
            java.util.List<com.wecureit.entity.AppointmentHistory> list = appointmentHistoryRepository.findByPatientId(patientId);
            if (list == null) return new java.util.ArrayList<>();
            return list;
        } catch (Exception ex) {
            System.out.println("getAppointmentHistoryForPatient: error: " + ex.getMessage());
            ex.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
}
