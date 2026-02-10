package com.wecureit.controller.patient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.entity.Patient;
import com.wecureit.repository.PatientRepository;

@RestController
@RequestMapping("/api/patient")
public class PatientLoginController {

    private final PatientRepository patientRepository;

    public PatientLoginController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String uid = authentication.getName();
        Map<String, Object> out = new HashMap<>();
        out.put("uid", uid);
        out.put("message", "Patient authenticated successfully");

        try {
            Optional<Patient> maybe = patientRepository.findByFirebaseUid(uid);
            if (maybe.isPresent()) {
                Patient p = maybe.get();
                // include the internal patient UUID so front-end can call patient-scoped APIs
                if (p.getId() != null) out.put("id", p.getId().toString());
                out.put("email", p.getEmail());
                // include secondaryEmail and address so profile UI can show edited email/address
                out.put("secondaryEmail", p.getSecondaryEmail());
                out.put("address", p.getAddress());
                // also include structured location fields so frontend can display city/state/zip
                out.put("city", p.getCity());
                out.put("state", p.getState());
                out.put("zip", p.getZip());
                // include contact and demographic fields for client display
                out.put("phone", p.getPhone());
                // frontend expects 'sex' key for display; entity stores gender
                out.put("sex", p.getGender());
                // dob as ISO string
                out.put("dob", p.getDob() == null ? null : p.getDob().toString());
                out.put("name", p.getName());
            }
        } catch (Exception e) {
            // do not block authentication response if DB lookup fails; log and continue
            System.err.println("PatientLoginController.me: failed to resolve patient by firebaseUid=" + uid + ": " + e.getMessage());
        }

        return ResponseEntity.ok(out);
    }

    @org.springframework.web.bind.annotation.PostMapping("/profile")
    public ResponseEntity<?> updateProfile(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> body, Authentication authentication) {
        String uid = authentication.getName();
        try {
            java.util.Optional<Patient> maybe = patientRepository.findByFirebaseUid(uid);
            if (maybe.isEmpty()) return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Patient not found"));
            Patient p = maybe.get();

            // update allowed fields. Do not touch primary email (login) here; store edited email in secondaryEmail
            if (body.containsKey("name") && body.get("name") != null) p.setName(String.valueOf(body.get("name")));
            if (body.containsKey("phone") && body.get("phone") != null) p.setPhone(String.valueOf(body.get("phone")));
            if (body.containsKey("city") && body.get("city") != null) p.setCity(String.valueOf(body.get("city")));
            if (body.containsKey("state") && body.get("state") != null) p.setState(String.valueOf(body.get("state")));
            if (body.containsKey("zip") && body.get("zip") != null) p.setZip(String.valueOf(body.get("zip")));
            if (body.containsKey("address") && body.get("address") != null) p.setAddress(String.valueOf(body.get("address")));
            if (body.containsKey("sex") && body.get("sex") != null) p.setGender(String.valueOf(body.get("sex")));
            if (body.containsKey("dob") && body.get("dob") != null) {
                try {
                    java.time.LocalDate parsed = java.time.LocalDate.parse(String.valueOf(body.get("dob")));
                    p.setDob(parsed);
                } catch (Exception ex) {
                    // ignore parse errors and do not update dob
                }
            }
            // edited email stored in secondaryEmail to avoid impacting login
            if (body.containsKey("email") && body.get("email") != null) p.setSecondaryEmail(String.valueOf(body.get("email")));

            patientRepository.save(p);

            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("id", p.getId() == null ? null : p.getId().toString());
            out.put("email", p.getEmail());
            out.put("secondaryEmail", p.getSecondaryEmail());
            out.put("address", p.getAddress());
            out.put("name", p.getName());
            out.put("phone", p.getPhone());
            out.put("city", p.getCity());
            out.put("state", p.getState());
            out.put("zip", p.getZip());
            out.put("sex", p.getGender());
            out.put("dob", p.getDob() == null ? null : p.getDob().toString());
            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            System.err.println("PatientLoginController.updateProfile: " + ex.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "Failed to update profile"));
        }
    }
}
