package com.wecureit.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.wecureit.dto.request.SignupRequest;
import com.wecureit.entity.Admin;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.Patient;
import com.wecureit.repository.AdminRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.PatientRepository;

@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;

    public AuthService(
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            AdminRepository adminRepository
    ) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.adminRepository = adminRepository;
    }

    public void signup(SignupRequest request) {

        String uid = null;
        boolean createdNewFirebaseUser = false;

        try {
            try {
                UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(
                        new UserRecord.CreateRequest()
                                .setEmail(request.getEmail())
                                .setPassword(request.getPassword())
                );

                uid = firebaseUser.getUid();
                createdNewFirebaseUser = true;
            } catch (FirebaseAuthException fae) {
                // If user already exists, look them up and continue. Different versions expose
                // different error detail APIs, so inspect the message text as a fallback.
                String msg = fae.getMessage() == null ? "" : fae.getMessage().toLowerCase();
                if (msg.contains("email") && msg.contains("exists")) {
                    UserRecord existing = FirebaseAuth.getInstance().getUserByEmail(request.getEmail());
                    uid = existing.getUid();
                } else {
                    // rethrow to outer handler
                    throw fae;
                }
            }

            // set role claim for the user (works for existing or newly created)
            FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", request.getRole()));

            // Persist role-specific entity. If DB save fails and we created the Firebase user above, roll it back.
            try {
                switch (request.getRole()) {
                    case "PATIENT" -> createPatient(uid, request.getEmail(), request);
                    case "DOCTOR" -> createDoctor(uid, request.getEmail());
                    case "ADMIN" -> createAdmin(uid, request.getEmail());
                    default -> throw new IllegalArgumentException("Invalid role");
                }
            } catch (Exception persistEx) {
                if (createdNewFirebaseUser && uid != null) {
                    try {
                        FirebaseAuth.getInstance().deleteUser(uid);
                    } catch (Exception ignore) {
                        // best-effort cleanup
                    }
                }
                throw persistEx;
            }

        } catch (Exception e) {
            throw new RuntimeException("Signup failed", e);
        }
    }

    public void createPatient(String firebaseUid, String email, SignupRequest request) {

        Patient patient = new Patient();
        // let JPA generate id (don't override unless necessary)
        patient.setFirebaseUid(firebaseUid);
        patient.setEmail(email);

        patient.setName(request.getName());
        patient.setPhone(request.getPhone());
        patient.setDob(request.getDob());
        patient.setGender(request.getGender());
        patient.setCity(request.getCity());
        patient.setState(request.getState());
        patient.setZip(request.getZip());

        patientRepository.save(patient);
    }

    private void createDoctor(String uid, String email) {
        Doctor doctor = new Doctor();
        doctor.setFirebaseUid(uid);
        doctor.setEmail(email);
        doctorRepository.save(doctor);
    }

    private void createAdmin(String uid, String email) {
        Admin admin = new Admin();
        admin.setFirebaseUid(uid);
        admin.setEmail(email);
        adminRepository.save(admin);
    }
}
