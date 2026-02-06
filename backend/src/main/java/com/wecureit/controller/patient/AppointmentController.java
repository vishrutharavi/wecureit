package com.wecureit.controller.patient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.AppointmentRequest;
import com.wecureit.dto.response.AppointmentResponse;
import com.wecureit.entity.Appointment;
import com.wecureit.service.AppointmentService;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.entity.Doctor;
import com.wecureit.repository.RoomRepository;
import com.wecureit.repository.PatientRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.wecureit.entity.Room;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final RoomRepository roomRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentController(AppointmentService appointmentService, RoomRepository roomRepository, DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.appointmentService = appointmentService;
        this.roomRepository = roomRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody AppointmentRequest req) {
        try {
            Appointment saved = appointmentService.createAppointment(req);
            URI location = URI.create("/appointments/" + saved.getId());
            return ResponseEntity.created(location).body(toResponse(saved));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", iae.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        Appointment appt = appointmentService.findById(id);
        return ResponseEntity.ok(toResponse(appt));
    }

    @GetMapping("/byPatient")
    public ResponseEntity<List<AppointmentResponse>> byPatient(@RequestParam(name = "patientId") UUID patientId) {
        var list = appointmentService.findByPatientId(patientId);
        var out = new ArrayList<AppointmentResponse>();
        for (Appointment a : list) out.add(toResponse(a));
        return ResponseEntity.ok(out);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestParam(name = "cancelledBy", required = false) String cancelledBy) {
        try {
            // Determine caller from security context and enforce patient ownership
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String callerUid = null;
            if (auth != null && auth.getPrincipal() instanceof String) {
                callerUid = (String) auth.getPrincipal();
            }
            String who = cancelledBy;
            System.out.println("AppointmentController.cancel - callerUid=" + callerUid + " cancelledByParam=" + cancelledBy);
            if (callerUid != null) {
                // debug: print principal class and value to help diagnose 403 issues
                try {
                    System.out.println("AppointmentController.cancel - auth principal class=" + (auth != null && auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null") + " principalValue=" + callerUid);
                } catch (Exception e) {
                    // ignore
                }
                // If caller is a patient record in DB, enforce ownership and set cancelledBy="patient"
                var patOpt = patientRepository.findByFirebaseUid(callerUid);
                if (patOpt.isPresent()) {
                    var pat = patOpt.get();
                    System.out.println("AppointmentController.cancel - resolved patient id=" + pat.getId());
                    // ensure appointment belongs to this patient
                    Appointment appt = appointmentService.findById(id);
                    System.out.println("AppointmentController.cancel - appointment.patient firebaseUid=" + (appt.getPatient() == null ? null : appt.getPatient().getFirebaseUid()));
                    System.out.println("AppointmentController.cancel - appointment.patientId=" + (appt.getPatient() == null ? null : appt.getPatient().getId()));
                    // First try comparing DB ids (primary key). As a fallback, also compare firebaseUid stored on the appointment.patient
                    boolean owner = false;
                    if (appt.getPatient() != null) {
                        try {
                            if (appt.getPatient().getId() != null && pat.getId() != null && appt.getPatient().getId().equals(pat.getId())) owner = true;
                        } catch (Exception e) {
                            // ignore and try firebaseUid fallback
                        }
                        if (!owner) {
                            try {
                                String apptPatientUid = appt.getPatient().getFirebaseUid();
                                if (apptPatientUid != null && apptPatientUid.equals(callerUid)) owner = true;
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                    if (!owner) {
                        System.out.println("AppointmentController.cancel - ownership mismatch, denying cancel (callerUid=" + callerUid + ")");
                        return ResponseEntity.status(403).body(java.util.Map.of("error", "You are not allowed to cancel this appointment"));
                    }
                    who = "patient";
                }
            }

            Appointment canceled = appointmentService.cancelAppointment(id, who);
            return ResponseEntity.ok(toResponse(canceled));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", iae.getMessage()));
        }
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
        if (a.getPatient() != null) resp.setPatientId(a.getPatient().getId());
        if (a.getDoctorAvailability() != null) resp.setDoctorAvailabilityId(a.getDoctorAvailability().getId());
        if (a.getFacility() != null) resp.setFacilityId(a.getFacility().getId());
        if (a.getSpeciality() != null) {
            resp.setSpecialityId(a.getSpeciality().getSpecialityCode());
            resp.setSpecialityName(a.getSpeciality().getSpecialityName());
        }
        if (a.getFacility() != null) {
            resp.setFacilityName(a.getFacility().getName());
        }
        // doctorName: try to resolve from doctorAvailability.doctorId -> Doctor
        if (a.getDoctorAvailability() != null && a.getDoctorAvailability().getDoctorId() != null) {
            try {
                var docOpt = doctorRepository.findById(a.getDoctorAvailability().getDoctorId());
                if (docOpt.isPresent()) {
                    Doctor d = docOpt.get();
                    resp.setDoctorName(d.getName());
                }
            } catch (Exception e) {
                // don't fail response on doctor lookup issues
            }
        }
        if (a.getRoomSchedule() != null) {
            resp.setRoomScheduleId(a.getRoomSchedule().getId());
            try {
                var roomId = a.getRoomSchedule().getRoomId();
                if (roomId != null) {
                    Room r = roomRepository.findById(roomId).orElse(null);
                    if (r != null) resp.setRoomNumber(r.getRoomNumber());
                }
            } catch (Exception e) {
                // don't fail response if room lookup has an issue; log if needed
            }
        }
        return resp;
    }
}
