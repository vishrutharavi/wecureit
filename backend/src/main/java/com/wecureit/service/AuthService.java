package com.wecureit.service;

import com.wecureit.dto.request.SignupRequest;
import com.wecureit.entity.Admin;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.Patient;
import com.wecureit.repository.AdminRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.PatientRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

import java.util.Map;

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

        try {
            // 1️⃣ Create Firebase user
            UserRecord firebaseUser =
                    FirebaseAuth.getInstance().createUser(
                            new UserRecord.CreateRequest()
                                    .setEmail(request.getEmail())
                                    .setPassword(request.getPassword())
                    );

            String uid = firebaseUser.getUid();

            // 2️⃣ Assign role as Firebase custom claim
            FirebaseAuth.getInstance().setCustomUserClaims(
                    uid,
                    Map.of("role", request.getRole())
            );

            // 3️⃣ Persist in role-specific table
            switch (request.getRole()) {
                case "PATIENT" -> createPatient(uid, request.getEmail());
                case "DOCTOR"  -> createDoctor(uid, request.getEmail());
                case "ADMIN"   -> createAdmin(uid, request.getEmail());
                default -> throw new IllegalArgumentException("Invalid role");
            }

        } catch (Exception e) {
            throw new RuntimeException("Signup failed", e);
        }
    }

    private void createPatient(String uid, String email) {
        Patient patient = new Patient();
        patient.setFirebaseUid(uid);
        patient.setEmail(email);
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
