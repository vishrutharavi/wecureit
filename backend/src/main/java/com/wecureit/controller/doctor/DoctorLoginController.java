package com.wecureit.controller.doctor;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.response.DoctorLoginResponse;
import com.wecureit.entity.Doctor;
import com.wecureit.repository.DoctorRepository;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctor")
public class DoctorLoginController {

    private final DoctorRepository doctorRepo;

    public DoctorLoginController(DoctorRepository doctorRepo) {
        this.doctorRepo = doctorRepo;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String uid = authentication.getName();
        // find doctor by firebase UID
        Optional<Doctor> maybe = doctorRepo.findByFirebaseUid(uid);
        if (maybe.isEmpty()) {
            // return basic info from token if no DB record exists
            return ResponseEntity.ok(new DoctorLoginResponse(null, uid, null, null, null));
        }

        Doctor d = maybe.get();
        DoctorLoginResponse resp = new DoctorLoginResponse(
            d.getId() != null ? d.getId().toString() : null,
            d.getFirebaseUid(),
            d.getEmail(),
            d.getName(),
            d.getGender()
        );

        return ResponseEntity.ok(resp);
    }

}
