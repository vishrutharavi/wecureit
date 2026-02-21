package com.wecureit.controller;

import java.util.Map;
import java.util.Optional;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.wecureit.dto.request.SignupRequest;
import com.wecureit.dto.response.StateResponse;
import com.wecureit.entity.Admin;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.Patient;
import com.wecureit.repository.AdminRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.PatientRepository;
import com.wecureit.service.AuthService;
import com.wecureit.service.StateService;

import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthService authService;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final AdminRepository adminRepo;
    private final StateService stateService;

    public AuthController(AuthService authService, DoctorRepository doctorRepo, PatientRepository patientRepo, AdminRepository adminRepo, StateService stateService) {
        this.authService = authService;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.adminRepo = adminRepo;
        this.stateService = stateService;
    }

    @GetMapping("/states")
    public List<StateResponse> getStates() {
        return stateService.getAllStates();
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signupPatient(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok().build();
}

    @PostMapping("/link")
    public ResponseEntity<?> linkPortalIdentity(@RequestBody Map<String, String> body, HttpServletRequest request) {
        System.out.println("AuthController.linkPortalIdentity invoked: remote=" + request.getRemoteAddr() + " uri=" + request.getRequestURI());
        System.out.println("AuthController.linkPortalIdentity: headers Authorization=" + request.getHeader("Authorization") + " Content-Type=" + request.getHeader("Content-Type"));
        String portal = body.get("portal");
        if (portal == null) return ResponseEntity.badRequest().body(Map.of("error", "portal is required (DOCTOR|PATIENT|ADMIN)"));
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return ResponseEntity.status(401).body(Map.of("error", "Missing Authorization token"));
        String idToken = auth.substring(7);
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decoded.getUid();
            String email = decoded.getEmail();
            if (email == null || email.isBlank()) return ResponseEntity.status(400).body(Map.of("error", "Token does not contain an email"));
            String role = portal.trim().toUpperCase();
            // normalize email and attempt case-insensitive lookup to avoid failures
            String emailNorm = email.trim();
            switch (role) {
                case "DOCTOR":
                    Optional<Doctor> dopt = doctorRepo.findByEmail(emailNorm);
                    boolean usedCi = false;
                    if (dopt.isEmpty()) {
                        dopt = doctorRepo.findByEmailIgnoreCase(emailNorm);
                        usedCi = dopt.isPresent();
                    }
                    if (dopt.isPresent()) {
                        Doctor d = dopt.get();
                        d.setFirebaseUid(uid);
                        doctorRepo.save(d);
                        System.out.println("AuthController.linkPortalIdentity: linked doctor email=" + d.getEmail() + " uid=" + uid + " (ciLookup=" + usedCi + ")");
                    } else {
                        // If no doctor record exists, create a minimal doctor entry so the portal can be linked.
                        Doctor newDoc = new Doctor();
                        newDoc.setFirebaseUid(uid);
                        newDoc.setEmail(emailNorm);
                        doctorRepo.save(newDoc);
                        System.out.println("AuthController.linkPortalIdentity: created new doctor record for email=" + emailNorm + " uid=" + uid);
                    }
                    break;
                case "PATIENT":
                    Optional<Patient> popt = patientRepo.findByEmail(emailNorm);
                    boolean patientUsedCi = false;
                    if (popt.isEmpty()) {
                        popt = patientRepo.findByEmailIgnoreCase(emailNorm);
                        patientUsedCi = popt.isPresent();
                    }
                    // also try secondaryEmail (case-insensitive) in case user updated their email in profile
                    boolean patientUsedSecondary = false;
                    if (popt.isEmpty()) {
                        popt = patientRepo.findBySecondaryEmailIgnoreCase(emailNorm);
                        patientUsedSecondary = popt.isPresent();
                    }
                    if (popt.isPresent()) {
                        Patient p = popt.get();
                        p.setFirebaseUid(uid);
                        patientRepo.save(p);
                        System.out.println("AuthController.linkPortalIdentity: linked patient email=" + p.getEmail() + " uid=" + uid + " (ciLookup=" + patientUsedCi + ", secondaryLookup=" + patientUsedSecondary + ")");
                    } else {
                        return ResponseEntity.status(404).body(Map.of("error", "No patient found with email " + emailNorm + ". Please create a patient record before linking."));
                    }
                    break;
                case "ADMIN":
                    Optional<Admin> aopt = adminRepo.findByEmail(emailNorm);
                    boolean adminUsedCi = false;
                    if (aopt.isEmpty()) {
                        aopt = adminRepo.findByEmailIgnoreCase(emailNorm);
                        adminUsedCi = aopt.isPresent();
                    }
                    if (aopt.isPresent()) {
                        Admin a = aopt.get();
                        a.setFirebaseUid(uid);
                        adminRepo.save(a);
                        System.out.println("AuthController.linkPortalIdentity: linked admin email=" + a.getEmail() + " uid=" + uid + " (ciLookup=" + adminUsedCi + ")");
                    } else {
                        return ResponseEntity.status(404).body(Map.of("error", "No admin found with email " + emailNorm));
                    }
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Unknown portal role: " + portal));
            }

            // Try to set custom claim so future tokens include role (best-effort)
            try {
                FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", role));
            } catch (Exception ex) {
                System.out.println("AuthController.linkPortalIdentity: failed to set custom claim: " + ex.getMessage());
            }

            return ResponseEntity.ok(Map.of("role", role));
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Token verification failed: " + ex.getMessage()));
        }
    }
}
