package com.wecureit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.request.CreateReferralRequest;
import com.wecureit.event.ReferralCreatedEvent;
import com.wecureit.event.ReferralStatusChangedEvent;
import com.wecureit.dto.response.RecommendedDoctorResponse;
import com.wecureit.dto.response.ReferralResponse;
import com.wecureit.entity.Appointment;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.entity.Patient;
import com.wecureit.entity.Referral;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.AppointmentRepository;
import com.wecureit.repository.DoctorAvailabilityRepository;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.PatientRepository;
import com.wecureit.repository.ReferralRepository;
import com.wecureit.repository.SpecialityRepository;
import com.wecureit.repository.StateRepository;

@Service
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final SpecialityRepository specialityRepository;
    private final DoctorLicenseRepository doctorLicenseRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final StateRepository stateRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReferralService(
            ReferralRepository referralRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            SpecialityRepository specialityRepository,
            DoctorLicenseRepository doctorLicenseRepository,
            DoctorAvailabilityRepository doctorAvailabilityRepository,
            StateRepository stateRepository,
            ApplicationEventPublisher eventPublisher) {
        this.referralRepository = referralRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.specialityRepository = specialityRepository;
        this.doctorLicenseRepository = doctorLicenseRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.stateRepository = stateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReferralResponse createReferral(UUID fromDoctorId, CreateReferralRequest req) {
        Doctor fromDoctor = doctorRepository.findById(fromDoctorId)
                .orElseThrow(() -> new RuntimeException("Referring doctor not found"));

        Doctor toDoctor = doctorRepository.findById(req.getToDoctorId())
                .orElseThrow(() -> new RuntimeException("Target doctor not found"));

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Speciality speciality = specialityRepository.findById(req.getSpecialityCode())
                .orElseThrow(() -> new RuntimeException("Speciality not found"));

        Referral referral = new Referral();
        referral.setFromDoctor(fromDoctor);
        referral.setToDoctor(toDoctor);
        referral.setPatient(patient);
        referral.setSpeciality(speciality);
        referral.setReason(req.getReason());
        referral.setStatus("PENDING");

        if (req.getAppointmentId() != null) {
            Appointment appointment = appointmentRepository.findById(req.getAppointmentId())
                    .orElse(null);
            referral.setAppointment(appointment);
        }

        referral = referralRepository.save(referral);

        eventPublisher.publishEvent(new ReferralCreatedEvent(
                referral.getId(),
                referral.getFromDoctor().getId(),
                referral.getToDoctor().getId(),
                referral.getPatient().getId(),
                referral.getSpeciality().getSpecialityCode(),
                referral.getReason()));

        return toResponse(referral);
    }

    public List<ReferralResponse> getOutgoingReferrals(UUID doctorId) {
        return referralRepository.findByFromDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReferralResponse> getIncomingReferrals(UUID doctorId) {
        return referralRepository.findByToDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void cancelReferral(UUID referralId, UUID doctorId, String cancelReason) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));

        if (!referral.getFromDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Only the referring doctor can cancel this referral");
        }

        String oldStatus = referral.getStatus();
        referral.setStatus("CANCELLED");
        referral.setCancelReason(cancelReason);
        referralRepository.save(referral);

        eventPublisher.publishEvent(new ReferralStatusChangedEvent(
                referralId, oldStatus, "CANCELLED"));
    }

    @Transactional
    public void acceptReferral(UUID referralId, UUID doctorId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));

        if (!referral.getToDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Only the receiving doctor can accept this referral");
        }
        if (!"PENDING".equals(referral.getStatus())) {
            throw new RuntimeException("Only pending referrals can be accepted");
        }

        referral.setStatus("ACCEPTED");
        referralRepository.save(referral);

        eventPublisher.publishEvent(new ReferralStatusChangedEvent(
                referralId, "PENDING", "ACCEPTED"));
    }

    @Transactional
    public void completeReferral(UUID referralId, UUID doctorId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new RuntimeException("Referral not found"));

        if (!referral.getToDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Only the receiving doctor can complete this referral");
        }
        if (!"ACCEPTED".equals(referral.getStatus())) {
            throw new RuntimeException("Only accepted referrals can be completed");
        }

        referral.setStatus("COMPLETED");
        referralRepository.save(referral);

        eventPublisher.publishEvent(new ReferralStatusChangedEvent(
                referralId, "ACCEPTED", "COMPLETED"));
    }

    public List<RecommendedDoctorResponse> getRecommendedDoctors(UUID patientId, String specialityCode) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        String patientStateCode = patient.getState() != null ? patient.getState().getStateCode() : null;

        // Find doctors with matching specialty and state license
        List<DoctorLicense> licenses = new ArrayList<>();
        if (patientStateCode != null) {
            licenses = doctorLicenseRepository.findByStateCodeAndSpecialityCodeAndIsActiveTrue(
                    patientStateCode, specialityCode);
        }

        LocalDate today = LocalDate.now();
        List<RecommendedDoctorResponse> recommendations = new ArrayList<>();

        for (DoctorLicense license : licenses) {
            Doctor doctor = doctorRepository.findById(license.getDoctorId()).orElse(null);
            if (doctor == null || !doctor.getIsActive()) continue;

            RecommendedDoctorResponse rec = new RecommendedDoctorResponse();
            rec.setDoctorId(doctor.getId());
            rec.setDoctorName(doctor.getName());
            rec.setDoctorEmail(doctor.getEmail());
            rec.setStateCode(license.getStateCode());

            // Resolve state name
            stateRepository.findById(license.getStateCode())
                    .ifPresent(state -> rec.setStateName(state.getStateName()));

            // Find next available slot
            List<DoctorAvailability> availabilities = doctorAvailabilityRepository
                    .findByDoctorIdAndWorkDateGreaterThanEqualAndIsActiveTrue(doctor.getId(), today);
            availabilities.sort(Comparator.comparing(DoctorAvailability::getWorkDate)
                    .thenComparing(DoctorAvailability::getStartTime));

            if (!availabilities.isEmpty()) {
                DoctorAvailability nextSlot = availabilities.get(0);
                rec.setNextAvailableSlot(nextSlot.getWorkDate().toString());
            } else {
                rec.setNextAvailableSlot("No upcoming slots");
            }

            // Count upcoming appointment load
            LocalDateTime startAt = today.atStartOfDay();
            LocalDateTime endAt = today.plusDays(30).atStartOfDay();
            List<Appointment> upcomingAppointments = appointmentRepository
                    .findAppointmentsForDoctor(doctor.getId(), startAt, endAt);
            int load = (int) upcomingAppointments.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count();
            rec.setAppointmentLoad(load);

            // Build reason string
            List<String> reasons = new ArrayList<>();
            reasons.add("Same state as patient");
            if (load < 5) reasons.add("Low appointment load");
            else if (load < 15) reasons.add("Moderate appointment load");
            else reasons.add("High appointment load");
            if (!availabilities.isEmpty()) {
                long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, availabilities.get(0).getWorkDate());
                if (daysUntil <= 3) reasons.add("Available soon");
            }
            rec.setReason(String.join(", ", reasons));

            // Compute 0-100 recommendation score (penalty-from-100 model)
            try {
                int score = 100;
                // Appointment load penalty: -2 per appointment, capped at -40
                score -= Math.min(40, load * 2);
                // Availability penalty: -1 per day until next slot, capped at -30
                // No slots at all: fixed -30 penalty
                if (availabilities.isEmpty()) {
                    score -= 30;
                } else {
                    long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                            today, availabilities.get(0).getWorkDate());
                    score -= (int) Math.min(30, daysUntil);
                }
                rec.setScore(Math.max(0, score));
            } catch (Exception e) {
                rec.setScore(50); // safe default if computation fails
            }

            recommendations.add(rec);
        }

        // Sort: soonest availability first, then lowest load
        recommendations.sort(Comparator
                .comparing((RecommendedDoctorResponse r) -> {
                    if ("No upcoming slots".equals(r.getNextAvailableSlot())) return LocalDate.MAX;
                    return LocalDate.parse(r.getNextAvailableSlot());
                })
                .thenComparingInt(RecommendedDoctorResponse::getAppointmentLoad));

        // Return top 5
        return recommendations.stream().limit(5).collect(Collectors.toList());
    }

    public List<ReferralResponse> getPatientReferrals(UUID patientId) {
        return referralRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<RecommendedDoctorResponse> searchDoctors(String query, String specialityCode) {
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query);

        List<RecommendedDoctorResponse> results = new ArrayList<>();
        for (Doctor doctor : doctors) {
            // If specialityCode is provided, check the doctor has a license for it
            if (specialityCode != null && !specialityCode.isEmpty()) {
                List<DoctorLicense> licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(doctor.getId());
                boolean hasSpeciality = licenses.stream()
                        .anyMatch(l -> l.getSpecialityCode().equals(specialityCode));
                if (!hasSpeciality) continue;
            }

            RecommendedDoctorResponse rec = new RecommendedDoctorResponse();
            rec.setDoctorId(doctor.getId());
            rec.setDoctorName(doctor.getName());
            rec.setDoctorEmail(doctor.getEmail());

            // Get first active license for state info
            List<DoctorLicense> licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(doctor.getId());
            if (!licenses.isEmpty()) {
                DoctorLicense first = licenses.get(0);
                rec.setStateCode(first.getStateCode());
                stateRepository.findById(first.getStateCode())
                        .ifPresent(state -> rec.setStateName(state.getStateName()));
            }

            rec.setReason("Manual search result");
            results.add(rec);
        }

        return results.stream().limit(10).collect(Collectors.toList());
    }

    private ReferralResponse toResponse(Referral referral) {
        ReferralResponse resp = new ReferralResponse();
        resp.setId(referral.getId());
        resp.setStatus(referral.getStatus());
        resp.setReason(referral.getReason());
        resp.setCancelReason(referral.getCancelReason());
        resp.setCreatedAt(referral.getCreatedAt());

        if (referral.getPatient() != null) {
            resp.setPatientId(referral.getPatient().getId());
            resp.setPatientName(referral.getPatient().getName());
        }

        if (referral.getFromDoctor() != null) {
            resp.setFromDoctorId(referral.getFromDoctor().getId());
            resp.setFromDoctorName(referral.getFromDoctor().getName());
            resp.setFromDoctorEmail(referral.getFromDoctor().getEmail());
        }

        if (referral.getToDoctor() != null) {
            resp.setToDoctorId(referral.getToDoctor().getId());
            resp.setToDoctorName(referral.getToDoctor().getName());
            resp.setToDoctorEmail(referral.getToDoctor().getEmail());
        }

        if (referral.getSpeciality() != null) {
            resp.setSpecialityCode(referral.getSpeciality().getSpecialityCode());
            resp.setSpecialityName(referral.getSpeciality().getSpecialityName());
        }

        if (referral.getAppointment() != null) {
            resp.setAppointmentId(referral.getAppointment().getId());
        }

        return resp;
    }
}
