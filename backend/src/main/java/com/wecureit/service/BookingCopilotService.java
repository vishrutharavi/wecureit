package com.wecureit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wecureit.dto.request.BookingIntent;
import com.wecureit.dto.response.BookingAvailabilityResponse;
import com.wecureit.dto.response.BookingAvailabilitySlot;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.SpecialityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.*;

import java.util.stream.Collectors;

@Service
public class BookingCopilotService {
    
    private static final String SYSTEM_PROMPT = """
        You are an assistant that extracts appointment booking intent from a patient message.
        
        TODAY'S DATE IS: %s
        
        Return ONLY valid minified JSON (no markdown, no extra text, no code blocks).
        Schema:
        {
         "specialty": string|null,
         "facilityName": string|null,
         "doctorName": string|null,
         "preferredDateStart": "YYYY-MM-DD"|null,
         "preferredDateEnd": "YYYY-MM-DD"|null,
         "preferredTimeStart": "HH:mm"|null,
         "preferredTimeEnd": "HH:mm"|null,
         "durationMinutes": 15|30|60|null
        }
        
        Rules:
        - TODAY IS %s. Use this as the reference date for all date calculations.
        - If user says "next week", set preferredDateStart to next Monday and preferredDateEnd to 7 days later (next Sunday).
        - If user says "next Tuesday", set preferredDateStart and preferredDateEnd to the same date (next Tuesday).
        - If user gives a date range like "Feb 15-20", set start and end accordingly.
        - All dates MUST be in the year 2026 or later, never use 2023 or past years.
        - If time is like "after 4pm", set preferredTimeStart to 16:00 and preferredTimeEnd to 20:00.
        - If time is like "before 3pm", set preferredTimeStart to 09:00 and preferredTimeEnd to 15:00.
        - If time is like "morning", set preferredTimeStart to 09:00 and preferredTimeEnd to 12:00.
        - If time is like "afternoon", set preferredTimeStart to 13:00 and preferredTimeEnd to 17:00.
        - If time is like "evening", set preferredTimeStart to 17:00 and preferredTimeEnd to 20:00.
        - If duration not specified, set durationMinutes to 30 by default.
        - Do not invent facility or doctor names; use null if unknown.
        - For specialty, normalize to common medical terms (e.g., "skin doctor" -> "Dermatology").
        """;
    
    private final AgentService agentService;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorLicenseRepository doctorLicenseRepository;
    private final PatientBookingService patientBookingService;
    private final ObjectMapper objectMapper;
    
    public BookingCopilotService(
        AgentService agentService,
        FacilityRepository facilityRepository,
        SpecialityRepository specialityRepository,
        DoctorRepository doctorRepository,
        DoctorLicenseRepository doctorLicenseRepository,
        PatientBookingService patientBookingService) {
    this.agentService = agentService;
    this.facilityRepository = facilityRepository;
    this.specialityRepository = specialityRepository;
    this.doctorRepository = doctorRepository;
    this.doctorLicenseRepository = doctorLicenseRepository;
    this.patientBookingService = patientBookingService;
    this.objectMapper = new ObjectMapper();
    }
    
    public BookingIntent interpretUtterance(String utterance) {
        String today = LocalDate.now().toString();
        String systemPrompt = String.format(SYSTEM_PROMPT, today, today);
        
        try {
            String response = agentService.chatWithSystemPrompt(systemPrompt, utterance);
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("Empty response from Ollama");
            }
            
            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\n|```\n|```", "").trim();
            }
            
            return objectMapper.readValue(cleaned, BookingIntent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse booking intent: " + e.getMessage(), e);
        }
    }
    
    public List<SlotSuggestion> suggestSlots(BookingIntent intent) {
        List<SlotSuggestion> suggestions = new ArrayList<>();
        
        String specialtyCode = resolveSpecialty(intent.getSpecialty());
        if (specialtyCode == null && intent.getSpecialty() != null) {
            return suggestions;
        }
        
        UUID facilityId = null;
        String facilityName = null;
        if (intent.getFacilityName() != null) {
            Facility facility = resolveFacility(intent.getFacilityName());
            if (facility != null) {
                facilityId = facility.getId();
                facilityName = facility.getName();
            } else {
                return suggestions;
            }
        }
        
        List<Doctor> doctors = findMatchingDoctors(specialtyCode, facilityId, intent.getDoctorName());
        
        LocalDate startDate = parseDate(intent.getPreferredDateStart(), LocalDate.now());
        LocalDate endDate = parseDate(intent.getPreferredDateEnd(), startDate.plusDays(14));
        LocalTime startTime = parseTime(intent.getPreferredTimeStart(), LocalTime.of(9, 0));
        LocalTime endTime = parseTime(intent.getPreferredTimeEnd(), LocalTime.of(20, 0));
        int duration = intent.getDurationMinutes() != null ? intent.getDurationMinutes() : 30;
        
        for (Doctor doctor : doctors) {
            List<SlotSuggestion> doctorSlots = findAvailableSlots(
                    doctor, facilityId, facilityName, specialtyCode,
                    startDate, endDate, startTime, endTime, duration);
            suggestions.addAll(doctorSlots);
            
            if (suggestions.size() >= 10) break;
        }
        
        suggestions.sort((a, b) -> {
            int dateCmp = a.getDate().compareTo(b.getDate());
            return dateCmp != 0 ? dateCmp : a.getStartTime().compareTo(b.getStartTime());
        });
        
        return suggestions.stream().limit(3).collect(Collectors.toList());
    }
    
    public List<SlotSuggestion> suggestAlternatives(BookingIntent intent) {
        LocalTime startTime = parseTime(intent.getPreferredTimeStart(), LocalTime.of(9, 0)).minusHours(2);
        LocalTime endTime = parseTime(intent.getPreferredTimeEnd(), LocalTime.of(20, 0)).plusHours(2);
        
        if (startTime.isBefore(LocalTime.of(8, 0))) startTime = LocalTime.of(8, 0);
        if (endTime.isAfter(LocalTime.of(21, 0))) endTime = LocalTime.of(21, 0);
        
        LocalDate startDate = parseDate(intent.getPreferredDateStart(), LocalDate.now());
        LocalDate endDate = parseDate(intent.getPreferredDateEnd(), startDate.plusDays(21));
        
        BookingIntent relaxed = new BookingIntent();
        relaxed.setSpecialty(intent.getSpecialty());
        relaxed.setFacilityName(null);
        relaxed.setDoctorName(intent.getDoctorName());
        relaxed.setPreferredDateStart(startDate.toString());
        relaxed.setPreferredDateEnd(endDate.toString());
        relaxed.setPreferredTimeStart(startTime.toString());
        relaxed.setPreferredTimeEnd(endTime.toString());
        relaxed.setDurationMinutes(intent.getDurationMinutes());
        
        return suggestSlots(relaxed);
    }
    
    private String resolveSpecialty(String specialtyInput) {
        if (specialtyInput == null) return null;
        
        List<Speciality> allSpecialties = specialityRepository.findAll();
        for (Speciality s : allSpecialties) {
            if (s.getSpecialityName().equalsIgnoreCase(specialtyInput) ||
                s.getSpecialityName().toLowerCase().contains(specialtyInput.toLowerCase())) {
                return s.getSpecialityCode();
            }
        }
        return null;
    }
    
    private Facility resolveFacility(String facilityInput) {
        if (facilityInput == null) return null;
        
        List<Facility> allFacilities = facilityRepository.findAll();
        for (Facility f : allFacilities) {
            if (f.getName().equalsIgnoreCase(facilityInput) ||
                f.getName().toLowerCase().contains(facilityInput.toLowerCase())) {
                return f;
            }
        }
        return null;
    }
    
    private List<Doctor> findMatchingDoctors(String specialtyCode, UUID facilityId, String doctorName) {
        List<Doctor> allDoctors = doctorRepository.findAll();
        List<Doctor> filtered = new ArrayList<>();
        
        for (Doctor doctor : allDoctors) {
            if (!doctor.getIsActive()) continue;
            
            if (specialtyCode != null) {
                List<DoctorLicense> licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(doctor.getId());
                boolean hasSpecialty = licenses.stream()
                        .anyMatch(l -> specialtyCode.equals(l.getSpecialityCode()));
                if (!hasSpecialty) continue;
            }
            
            if (doctorName != null && doctor.getName() != null) {
                if (!doctor.getName().toLowerCase().contains(doctorName.toLowerCase())) {
                    continue;
                }
            }
            
            filtered.add(doctor);
        }
        
        return filtered;
    }
    
    private List<SlotSuggestion> findAvailableSlots(
            Doctor doctor, UUID facilityId, String facilityName, String specialtyCode,
            LocalDate startDate, LocalDate endDate,
            LocalTime startTime, LocalTime endTime, int duration) {
        
        List<SlotSuggestion> slots = new ArrayList<>();
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate) && slots.size() < 5) {
            try {
                BookingAvailabilityResponse availability = 
                        patientBookingService.getAvailabilitySlots(doctor.getId(), facilityId, current, duration);
                
                for (BookingAvailabilitySlot slot : availability.getSlots()) {
                    if (!"AVAILABLE".equals(slot.getStatus())) continue;
                    
                    LocalTime slotTime = LocalTime.parse(slot.getStartAt().substring(11, 16));
                    if (slotTime.isBefore(startTime) || slotTime.isAfter(endTime)) continue;
                    
                    String reason = buildReason(doctor, facilityName, specialtyCode, current, slotTime);
                    
                    slots.add(new SlotSuggestion(
                            doctor.getId().toString(),
                            doctor.getName(),
                            getSpecialtyName(specialtyCode),
                            facilityId != null ? facilityId.toString() : null,
                            facilityName,
                            current.toString(),
                            slot.getStartAt().substring(11, 16),
                            slot.getEndAt().substring(11, 16),
                            duration,
                            reason
                    ));
                    
                    if (slots.size() >= 5) break;
                }
            } catch (Exception e) {
                // Skip this date if error
            }
            current = current.plusDays(1);
        }
        
        return slots;
    }
    
    private String buildReason(Doctor doctor, String facilityName, String specialtyCode, 
                               LocalDate date, LocalTime time) {
        StringBuilder reason = new StringBuilder("Matches your ");
        
        if (specialtyCode != null) {
            reason.append(getSpecialtyName(specialtyCode)).append(" specialty");
        } else {
            reason.append("requirements");
        }
        
        if (facilityName != null) {
            reason.append(" at ").append(facilityName);
        }
        
        reason.append(" on ").append(date.format(DateTimeFormatter.ofPattern("EEE, MMM d")));
        reason.append(" at ").append(time.format(DateTimeFormatter.ofPattern("h:mm a")));
        
        return reason.toString();
    }
    
    private String getSpecialtyName(String specialtyCode) {
        if (specialtyCode == null) return "General";
        return specialityRepository.findById(specialtyCode)
                .map(Speciality::getSpecialityName)
                .orElse("General");
    }
    
    private LocalDate parseDate(String dateStr, LocalDate defaultValue) {
        if (dateStr == null || dateStr.trim().isEmpty()) return defaultValue;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private LocalTime parseTime(String timeStr, LocalTime defaultValue) {
        if (timeStr == null || timeStr.trim().isEmpty()) return defaultValue;
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
