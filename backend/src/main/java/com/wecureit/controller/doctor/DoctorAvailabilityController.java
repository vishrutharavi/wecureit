package com.wecureit.controller.doctor;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wecureit.dto.request.AvailabilityRequest;
import com.wecureit.dto.response.AvailabilityResponse;
import com.wecureit.service.DoctorAvailabilityService;
import com.wecureit.repository.DoctorRepository;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/doctors")
public class DoctorAvailabilityController {

    @Autowired
    private DoctorAvailabilityService availabilityService;
    private final DoctorRepository doctorRepository;

    public DoctorAvailabilityController(DoctorAvailabilityService availabilityService, DoctorRepository doctorRepository) {
        this.availabilityService = availabilityService;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping("/{doctorId}/availability")
    public ResponseEntity<List<AvailabilityResponse>> saveAvailability(
        @PathVariable("doctorId") UUID doctorId,
        @RequestBody List<AvailabilityRequest> items,
        Authentication authentication
    ) {
        UUID effectiveDoctorId = resolveDoctorId(doctorId, authentication);
        List<AvailabilityResponse> out = availabilityService.saveAvailabilities(effectiveDoctorId, items);
        return ResponseEntity.ok(out);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{doctorId}/availability")
    public ResponseEntity<List<AvailabilityResponse>> listAvailability(
        @PathVariable("doctorId") UUID doctorId,
        @org.springframework.web.bind.annotation.RequestParam(value = "from", required = false) String from,
        @org.springframework.web.bind.annotation.RequestParam(value = "to", required = false) String to
    ) {
        java.time.LocalDate fromDate = (from == null || from.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(from);
        java.time.LocalDate toDate = (to == null || to.isBlank()) ? fromDate.plusDays(30) : java.time.LocalDate.parse(to);
        // If caller is authenticated, prefer the backend-resolved doctor id to avoid stale client ids
        UUID effectiveDoctorId = doctorId;
        try {
            // resolve using authentication if available from security context
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                var maybe = doctorRepository.findByFirebaseUid(auth.getName());
                if (maybe.isPresent() && maybe.get().getId() != null) effectiveDoctorId = maybe.get().getId();
            }
        } catch (Exception ex) {
            // fallback to path variable
        }
        List<AvailabilityResponse> out = availabilityService.listAvailabilities(effectiveDoctorId, fromDate, toDate);
        return ResponseEntity.ok(out);
    }

    // Room assignment and walk-in toggling endpoints removed — rooms are not allocated from the API at this time.

    @org.springframework.web.bind.annotation.DeleteMapping("/{doctorId}/availability/{availabilityId}/delete-availability")
    public ResponseEntity<?> deleteAvailability(
        @PathVariable("doctorId") UUID doctorId,
        @PathVariable("availabilityId") UUID availabilityId
    ) {
        // prefer backend-resolved doctor id when authenticated
        UUID effectiveDoctorId = doctorId;
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                var maybe = doctorRepository.findByFirebaseUid(auth.getName());
                if (maybe.isPresent() && maybe.get().getId() != null) effectiveDoctorId = maybe.get().getId();
            }
        } catch (Exception ex) {
            // ignore
        }
        availabilityService.deleteAvailability(effectiveDoctorId, availabilityId);
        return ResponseEntity.ok().build();
    }

    private UUID resolveDoctorId(UUID pathDoctorId, Authentication authentication) {
        try {
            if (authentication != null && authentication.getName() != null) {
                var maybe = doctorRepository.findByFirebaseUid(authentication.getName());
                if (maybe.isPresent() && maybe.get().getId() != null) return maybe.get().getId();
            }
        } catch (Exception ex) {
            // ignore and fallback to path id
        }
        return pathDoctorId;
    }
}
