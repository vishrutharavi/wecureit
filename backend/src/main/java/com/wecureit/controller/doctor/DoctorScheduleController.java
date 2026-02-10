package com.wecureit.controller.doctor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.AppointmentResponse;
import com.wecureit.dto.response.DoctorScheduleResponse;
import com.wecureit.entity.Appointment;
import com.wecureit.entity.Doctor;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.service.AppointmentService;

@RestController
@RequestMapping("/api/doctors")
public class DoctorScheduleController {

    private final AppointmentService appointmentService;
    private final DoctorRepository doctorRepository;
    private final com.wecureit.repository.AppointmentHistoryRepository appointmentHistoryRepository;
    private final com.wecureit.repository.AppointmentRepository appointmentRepository;
    private final com.wecureit.repository.DoctorAvailabilityRepository doctorAvailabilityRepository;

    public DoctorScheduleController(AppointmentService appointmentService, DoctorRepository doctorRepository, com.wecureit.repository.AppointmentHistoryRepository appointmentHistoryRepository, com.wecureit.repository.AppointmentRepository appointmentRepository, com.wecureit.repository.DoctorAvailabilityRepository doctorAvailabilityRepository) {
        this.appointmentService = appointmentService;
        this.doctorRepository = doctorRepository;
        this.appointmentHistoryRepository = appointmentHistoryRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
    }

    @GetMapping("/{doctorId}/completed-appointments")
    public ResponseEntity<java.util.List<AppointmentResponse>> getCompletedAppointments(
            @PathVariable("doctorId") UUID doctorId,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr
    ) {
        java.time.LocalDate start = (startDateStr == null || startDateStr.isBlank()) ? java.time.LocalDate.now().minusDays(14) : java.time.LocalDate.parse(startDateStr);
        java.time.LocalDate end = (endDateStr == null || endDateStr.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(endDateStr);

        java.util.List<AppointmentResponse> out = new java.util.ArrayList<>();
        try {
            var histories = appointmentHistoryRepository.findByStatusIgnoreCase("completed");
            if (histories != null) {
                for (var h : histories) {
                    try {
                        if (h == null || h.getAppointmentId() == null) continue;
                        var apptOpt = appointmentRepository.findByUuid(h.getAppointmentId());
                        if (apptOpt.isPresent()) {
                            var a = apptOpt.get();
                            boolean matchesDoctor = false;
                            try {
                                if (a.getDoctorAvailability() != null && a.getDoctorAvailability().getDoctorId() != null) {
                                    matchesDoctor = a.getDoctorAvailability().getDoctorId().equals(doctorId);
                                }
                            } catch (Exception e) { /* ignore */ }
                            // fallback: appointment_history row may contain the doctor_availability_id
                            if (!matchesDoctor && h.getDoctorAvailabilityId() != null) {
                                try {
                                    var daOpt = doctorAvailabilityRepository.findById(h.getDoctorAvailabilityId());
                                    if (daOpt.isPresent() && daOpt.get().getDoctorId() != null) {
                                        matchesDoctor = daOpt.get().getDoctorId().equals(doctorId);
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }
                            if (!matchesDoctor) continue;
                            java.time.LocalDate d = a.getDate();
                            if (d == null) continue;
                            if ((d.isBefore(start)) || (d.isAfter(end))) continue;
                            var resp = toResponse(a);
                            resp.setStatus("COMPLETED");
                            out.add(resp);
                        }
                    } catch (Exception ex) {
                        // ignore per-row failures
                    }
                }
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Collections.emptyList());
        }

        return ResponseEntity.ok(out);
    }

    @org.springframework.web.bind.annotation.PostMapping("/{doctorId}/appointments/{id}/complete")
    public org.springframework.http.ResponseEntity<?> completeAppointment(
            @org.springframework.web.bind.annotation.PathVariable("doctorId") java.util.UUID doctorId,
            @org.springframework.web.bind.annotation.PathVariable("id") Long appointmentId) {
        try {
            Appointment a = appointmentService.markAppointmentCompleted(appointmentId, doctorId);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("ok", true, "appointmentId", a.getId()));
        } catch (IllegalArgumentException iae) {
            return org.springframework.http.ResponseEntity.status(403).body(java.util.Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{doctorId}/schedule")
    public ResponseEntity<DoctorScheduleResponse> getSchedule(
            @PathVariable("doctorId") UUID doctorId,
            @RequestParam(value = "date", required = false) String dateStr
    ) {
        java.time.LocalDate date = (dateStr == null || dateStr.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(dateStr);
        // include cancelled appointments so doctors can view both upcoming and cancelled
        List<Appointment> appts = appointmentService.getAppointmentsForDoctorAndDayIncludeCancelled(doctorId, date);
        // Determine explicit status for each appointment using appointment_history when available.
        java.util.Set<Long> completedIds = new java.util.HashSet<>();
        // track cancelled actor for appointments (patient/doctor/unknown)
        java.util.Map<Long, String> cancelledByMap = new java.util.HashMap<>();
        try {
            for (Appointment a : appts) {
                try {
                    if (a.getUuid() == null) continue;
                    var histories = appointmentHistoryRepository.findByAppointmentId(a.getUuid());
                    if (histories != null) {
                        for (var h : histories) {
                            if (h != null && h.getStatus() != null && h.getStatus().equalsIgnoreCase("completed")) {
                                completedIds.add(a.getId());
                                break;
                            }
                            if (h != null && h.getStatus() != null && h.getStatus().equalsIgnoreCase("cancelled") && h.getCancelledBy() != null) {
                                // store cancelled-by actor (normalized string such as 'patient' or 'doctor')
                                cancelledByMap.put(a.getId(), h.getCancelledBy());
                                // continue scanning - completed takes precedence when present
                            }
                        }
                    }
                } catch (Exception ex) {
                    // ignore per-appointment history errors but continue defensively
                    System.out.println("DoctorScheduleController: history check failed for appt " + (a == null ? "null" : a.getId()) + ": " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.out.println("DoctorScheduleController: failed while scanning histories: " + ex.getMessage());
        }
        List<AppointmentResponse> out = new ArrayList<>();
        for (Appointment a : appts) {
            AppointmentResponse resp = toResponse(a);
            try {
                if (completedIds.contains(a.getId())) {
                    resp.setStatus("COMPLETED");
                } else {
                    boolean active = a.getIsActive() == null ? true : a.getIsActive().booleanValue();
                    resp.setStatus(active ? "UPCOMING" : "CANCELLED");
                }
            } catch (Exception ex) {
                resp.setStatus(a.getIsActive() == null || a.getIsActive() ? "UPCOMING" : "CANCELLED");
            }
            // if a cancelled actor was recorded in history, propagate it so UI can show who cancelled
            try {
                if (cancelledByMap.containsKey(a.getId())) resp.setCancelledBy(cancelledByMap.get(a.getId()));
            } catch (Exception e) { /* ignore */ }
            out.add(resp);
        }

        // doctor_breaks are not persisted; doctor portal shows appointments only for now
        var schedule = new DoctorScheduleResponse(out);
        return ResponseEntity.ok(schedule);
    }

    private AppointmentResponse toResponse(Appointment a) {
        var resp = new AppointmentResponse();
        resp.setId(a.getId());
        resp.setDate(a.getDate());
        resp.setDuration(a.getDuration());
        resp.setStartTime(a.getStartTime());
        resp.setEndTime(a.getEndTime());
        resp.setIsActive(a.getIsActive());
        resp.setChiefComplaints(a.getChiefComplaints());
        if (a.getPatient() != null) {
            resp.setPatientId(a.getPatient().getId());
            try { resp.setPatientName(a.getPatient().getName()); } catch (Exception e) { /* ignore */ }
        }
        if (a.getDoctorAvailability() != null) resp.setDoctorAvailabilityId(a.getDoctorAvailability().getId());
        if (a.getFacility() != null) resp.setFacilityId(a.getFacility().getId());
        if (a.getSpeciality() != null) {
            resp.setSpecialityId(a.getSpeciality().getSpecialityCode());
            resp.setSpecialityName(a.getSpeciality().getSpecialityName());
        }
        if (a.getFacility() != null) {
            resp.setFacilityName(a.getFacility().getName());
        }
        if (a.getDoctorAvailability() != null && a.getDoctorAvailability().getDoctorId() != null) {
            try {
                var docOpt = doctorRepository.findById(a.getDoctorAvailability().getDoctorId());
                if (docOpt.isPresent()) {
                    Doctor d = docOpt.get();
                    resp.setDoctorName(d.getName());
                }
            } catch (Exception e) {
                // ignore
            }
        }
        // If appointment has explicit completed history, surface that as status; otherwise derive from isActive
        try {
            if (a.getId() != null && a.getId() > 0 && a.getId() != null) {
                // The caller will replace this with the computed status; default to null here.
            }
        } catch (Exception e) {
            // ignore
        }
        // status will be set by caller where we have access to computed completedIds map
        return resp;
    }
}
