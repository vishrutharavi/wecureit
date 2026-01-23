package com.wecureit.controller.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.CreateDoctorRequest;
import com.wecureit.entity.Doctor;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.service.AdminDoctorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private final AdminDoctorService service;
    private final DoctorRepository doctorRepo;

    public AdminDoctorController(
        AdminDoctorService service,
        DoctorRepository doctorRepo
    ) {
        this.service = service;
        this.doctorRepo = doctorRepo;
    }

    @PostMapping
    public ResponseEntity<?> createDoctor(
        @Valid @RequestBody CreateDoctorRequest req
    ) throws Exception {
        Doctor doctor = service.createDoctor(req);
        return ResponseEntity.ok(doctor);
    }

    @GetMapping
    public List<Doctor> getDoctors() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            System.out.println("AdminDoctorController.getDoctors - principal=" + auth.getPrincipal() + " authorities=" + auth.getAuthorities());
        } catch (Exception e) {
            System.out.println("AdminDoctorController.getDoctors - no authentication present");
        }

        var list = doctorRepo.findByIsActiveTrue();
        System.out.println("AdminDoctorController.getDoctors - returning " + list.size() + " active doctors");
        return list;
    }

    @PatchMapping("/{doctorId}")
    public ResponseEntity<?> deleteDoctor(@PathVariable UUID doctorId) {
        Optional<Doctor> maybe = doctorRepo.findById(doctorId);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Doctor not found"));
        }

        Doctor d = maybe.get();
        d.setIsActive(false);
        doctorRepo.save(d);
        return ResponseEntity.ok(Map.of("message", "Doctor deactivated"));
    }
    
    @PatchMapping("/{doctorId}/delete")
    public ResponseEntity<?> patchDeleteDoctor(@PathVariable UUID doctorId) {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            System.out.println("AdminDoctorController.patchDeleteDoctor - principal=" + auth.getPrincipal() + " authorities=" + auth.getAuthorities());
        } catch (Exception e) {
            System.out.println("AdminDoctorController.patchDeleteDoctor - no authentication present");
        }
        Optional<Doctor> maybe = doctorRepo.findById(doctorId);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Doctor not found"));
        }
        
        Doctor d = maybe.get();
        d.setIsActive(false);
        doctorRepo.save(d);
        return ResponseEntity.ok(Map.of("message", "Doctor deactivated"));
    }
}
