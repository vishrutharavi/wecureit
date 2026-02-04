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
                out.put("name", p.getName());
            }
        } catch (Exception e) {
            // do not block authentication response if DB lookup fails; log and continue
            System.err.println("PatientLoginController.me: failed to resolve patient by firebaseUid=" + uid + ": " + e.getMessage());
        }

        return ResponseEntity.ok(out);
    }
}
