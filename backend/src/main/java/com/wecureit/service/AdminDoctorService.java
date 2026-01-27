package com.wecureit.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.wecureit.dto.request.CreateDoctorRequest;
import com.wecureit.entity.Doctor;
import com.wecureit.repository.DoctorRepository;

@Service
public class AdminDoctorService {

    private final DoctorRepository doctorRepo;

    public AdminDoctorService(DoctorRepository doctorRepo) {
        this.doctorRepo = doctorRepo;
    }

    @Transactional
    public Doctor createDoctor(CreateDoctorRequest req) throws Exception {
        UserRecord user;
        try {
            // Attempt to create a new Firebase user with the provided password
            user = FirebaseAuth.getInstance().createUser(
                new UserRecord.CreateRequest()
                    .setEmail(req.getEmail())
                    .setPassword(req.getPassword())
            );
        } catch (com.google.firebase.auth.FirebaseAuthException fae) {
            // If the email already exists in Firebase, fetch that user instead of failing
            String msg = fae.getMessage() == null ? "" : fae.getMessage();
            if (msg.contains("EMAIL_EXISTS") || msg.contains("email already exists")) {
                user = FirebaseAuth.getInstance().getUserByEmail(req.getEmail());
            } else {
                // rethrow for other Firebase errors
                throw fae;
            }
        }

        // Ensure the user has the DOCTOR role claim
        FirebaseAuth.getInstance().setCustomUserClaims(
            user.getUid(),
            java.util.Map.of("role", "DOCTOR")
        );

        // If a doctor record with this firebase uid already exists, return it
        var existing = doctorRepo.findByFirebaseUid(user.getUid());
        if (existing.isPresent()) {
            // If a doctor with this Firebase UID already exists, surface a 409 to the caller
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doctor already exists");
        }

        // Save new doctor details in DB
    Doctor d = new Doctor();
        d.setFirebaseUid(user.getUid());
        d.setEmail(req.getEmail());
        d.setName(req.getName());
        d.setGender(req.getGender());

        return doctorRepo.save(d);
    }
}

