package com.wecureit.controller;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.entity.ClinicalNote;
import com.wecureit.repository.ClinicalNoteRepository;
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.AppointmentHistoryRepository;
import com.wecureit.repository.PatientRepository;
import com.wecureit.entity.Patient;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/clinical-notes")
public class ClinicalNoteController {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository appointmentHistoryRepository;
    private final PatientRepository patientRepository;

    public ClinicalNoteController(ClinicalNoteRepository clinicalNoteRepository, AppointmentRepository appointmentRepository, AppointmentHistoryRepository appointmentHistoryRepository, PatientRepository patientRepository) {
        this.clinicalNoteRepository = clinicalNoteRepository;
        this.appointmentRepository = appointmentRepository;
        this.appointmentHistoryRepository = appointmentHistoryRepository;
        this.patientRepository = patientRepository;
    }

    // Convert ClinicalNote entity into a Map with optional patient details (age, sex, name)
    private Map<String, Object> enrichNote(ClinicalNote n) {
        Map<String, Object> out = new HashMap<>();
        if (n.getId() != null) out.put("id", String.valueOf(n.getId()));
        if (n.getAppointmentHistoryId() != null) out.put("appointmentHistoryId", String.valueOf(n.getAppointmentHistoryId()));
        if (n.getAppointmentId() != null) out.put("appointmentId", String.valueOf(n.getAppointmentId()));
        if (n.getPatientId() != null) out.put("patientId", String.valueOf(n.getPatientId()));
        if (n.getDoctorId() != null) out.put("doctorId", String.valueOf(n.getDoctorId()));
        out.put("noteText", n.getNoteText());
        out.put("createdBy", n.getCreatedBy());
        out.put("createdAt", n.getCreatedAt() != null ? String.valueOf(n.getCreatedAt()) : null);
        out.put("updatedAt", n.getUpdatedAt() != null ? String.valueOf(n.getUpdatedAt()) : null);

        if (n.getPatientId() != null) {
            try {
                Optional<Patient> maybe = patientRepository.findById(n.getPatientId());
                if (maybe.isPresent()) {
                    Patient p = maybe.get();
                    out.put("patientName", p.getName());
                    if (p.getDob() != null) {
                        try {
                            int years = Period.between(p.getDob(), LocalDate.now()).getYears();
                            out.put("patientAge", years + " years old");
                        } catch (Exception ex) { /* ignore dob parsing */ }
                    }
                    out.put("patientSex", p.getGender());
                }
            } catch (Exception ex) {
                // ignore patient lookup errors
            }
        }
        return out;
    }

    @PostMapping("")
    public ResponseEntity<?> createNote(@RequestBody Map<String, Object> body) {
        try {
            ClinicalNote n = new ClinicalNote();
            // Accept either an appointment UUID (appointmentId) or numeric DB id (appointmentDbId)
            if (body.containsKey("appointmentDbId") && body.get("appointmentDbId") != null) {
                try {
                    Long dbId = Long.valueOf(String.valueOf(body.get("appointmentDbId")));
                    var maybe = appointmentRepository.findById(dbId);
                    if (maybe.isPresent()) {
                        var appt = maybe.get();
                        if (appt.getUuid() != null) n.setAppointmentId(appt.getUuid());
                        // attempt to find a recent appointment_history row for this appointment UUID
                        try {
                            if (appt.getUuid() != null) {
                                var histories = appointmentHistoryRepository.findByAppointmentId(appt.getUuid());
                                if (histories != null && !histories.isEmpty()) {
                                    // pick the latest by insertion order (repository returns by PK order) - take last
                                    n.setAppointmentHistoryId(histories.get(histories.size()-1).getId());
                                }
                            }
                        } catch (Exception hx) { /* ignore missing histories */ }
                    }
                } catch (Exception ex) { /* ignore */ }
            }
            if (body.containsKey("appointmentHistoryId") && body.get("appointmentHistoryId") != null) {
                n.setAppointmentHistoryId(UUID.fromString(String.valueOf(body.get("appointmentHistoryId"))));
            }
            if (body.containsKey("appointmentId") && body.get("appointmentId") != null) {
                // appointmentId as UUID
                n.setAppointmentId(UUID.fromString(String.valueOf(body.get("appointmentId"))));
            }
            if (body.containsKey("patientId") && body.get("patientId") != null) {
                n.setPatientId(UUID.fromString(String.valueOf(body.get("patientId"))));
            }
            if (body.containsKey("doctorId") && body.get("doctorId") != null) {
                n.setDoctorId(UUID.fromString(String.valueOf(body.get("doctorId"))));
            }
            if (body.containsKey("noteText") && body.get("noteText") != null) {
                n.setNoteText(String.valueOf(body.get("noteText")));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "noteText required"));
            }
            if (body.containsKey("createdBy") && body.get("createdBy") != null) n.setCreatedBy(String.valueOf(body.get("createdBy")));
            OffsetDateTime now = OffsetDateTime.now();
            n.setCreatedAt(now);
            n.setUpdatedAt(now);
            ClinicalNote saved = clinicalNoteRepository.save(n);
            return ResponseEntity.ok(enrichNote(saved));
        } catch (Exception ex) {
            System.err.println("ClinicalNoteController.createNote: " + ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create note"));
        }
    }

    @GetMapping("")
    public ResponseEntity<?> listNotes(@RequestParam(value = "appointmentHistoryId", required = false) String appointmentHistoryId,
                                       @RequestParam(value = "appointmentId", required = false) String appointmentId,
                                       @RequestParam(value = "patientId", required = false) String patientId,
                                       @RequestParam(value = "appointmentDbId", required = false) String appointmentDbId) {
        try {
            // allow fetching notes by numeric DB id for convenience (resolve to appointment.uuid)
            if (appointmentDbId != null && !appointmentDbId.isBlank()) {
                try {
                    Long dbId = Long.valueOf(appointmentDbId);
                    var maybe = appointmentRepository.findById(dbId);
                    if (maybe.isPresent()) {
                        var appt = maybe.get();
                        if (appt.getUuid() != null) {
                            var list = clinicalNoteRepository.findByAppointmentId(appt.getUuid());
                                // enrich with patient info where possible
                                var enriched = list.stream().map(n -> enrichNote(n)).toList();
                                return ResponseEntity.ok(enriched);
                        }
                    }
                } catch (Exception ex) {
                    // ignore and fall through to other filters
                }
            }
            if (appointmentHistoryId != null && !appointmentHistoryId.isBlank()) {
                var list = clinicalNoteRepository.findByAppointmentHistoryId(UUID.fromString(appointmentHistoryId));
                var enriched = list.stream().map(n -> enrichNote(n)).toList();
                return ResponseEntity.ok(enriched);
            }
            if (appointmentId != null && !appointmentId.isBlank()) {
                var list = clinicalNoteRepository.findByAppointmentId(UUID.fromString(appointmentId));
                var enriched = list.stream().map(n -> enrichNote(n)).toList();
                return ResponseEntity.ok(enriched);
            }
            if (patientId != null && !patientId.isBlank()) {
                var list = clinicalNoteRepository.findByPatientId(UUID.fromString(patientId));
                var enriched = list.stream().map(n -> enrichNote(n)).toList();
                return ResponseEntity.ok(enriched);
            }
            var list = clinicalNoteRepository.findAll();
            var enriched = list.stream().map(n -> enrichNote(n)).toList();
            return ResponseEntity.ok(enriched);
        } catch (Exception ex) {
            System.err.println("ClinicalNoteController.listNotes: " + ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to list notes"));
        }
    }
}
