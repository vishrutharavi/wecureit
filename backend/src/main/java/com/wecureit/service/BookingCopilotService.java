package com.wecureit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wecureit.dto.request.BookingIntent;
import com.wecureit.dto.response.BookingAvailabilityResponse;
import com.wecureit.dto.response.BookingAvailabilitySlot;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.DoctorAvailability;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.DoctorAvailabilityRepository;
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

            Return ONLY valid minified JSON (no markdown, no extra text).
            Schema:
            {"specialty":string|null,"facilityName":string|null,"doctorName":string|null,"preferredDateStart":"YYYY-MM-DD"|null,"preferredDateEnd":"YYYY-MM-DD"|null,"preferredTimeStart":"HH:mm"|null,"preferredTimeEnd":"HH:mm"|null,"durationMinutes":15|30|60|null}

            Rules (STRICT):

            GENERAL
            - TODAY is %s. Use it for all relative date calculations.
            - Output MUST be valid JSON and MUST be minified (one line).
            - Never invent doctorName or facilityName. Only populate them if the user explicitly states them.
            - If something is ambiguous/uncertain, set it to null (do not guess).
            - All dates must be 2026 or later. If a parsed date would be before 2026-01-01, set it to null unless it can be rolled forward to 2026+ using the rules below.

            SPECIALTY
            - If user indicates a specialty, ALWAYS normalize to EXACT full names from this list ONLY:
            - Mapping (keywords on left -> ALWAYS output the right side):
            - "Dermatology": recognizes "skin", "derma", "acne", "rash", "eczema", "psoriasis", "mole", "wart"
            - "Cardiology": recognizes "heart", "cardio", "chest pain", "arrhythmia", "hypertension", "cardiac"
            - "Pediatrics": recognizes "children", "baby", "infant", "kid", "toddler", "child"
            - "Orthopedics": recognizes "bones", "joint", "fracture", "bone", "sprain", "orthopedic", "knee", "shoulder", "wrist", "ankle"
            - "Neurology": recognizes "brain", "neuro", "nervous", "migraine", "headache", "dizzy", "dizziness", "seizure", "nerve", "neurological"
            - "Surgery": recognizes "surgery", "surgical", "operate", "operation", "surgeon", "surgical procedure"
            - "General Practice": recognizes "general", "checkup", "check-up", "physical", "general health", "routine"
            - "Psychiatry": recognizes "mental health", "psychology", "therapy", "counseling", "depression", "anxiety", "psychiatric"
            - "Otolaryngology": recognizes "ear", "nose", "throat", "ent", "hearing", "rhinology", "otolaryngology"
            - "Gynecology": recognizes "gynecology", "gynae", "women", "reproductive", "women's health"
            - "Gastroenterology": recognizes "stomach", "digestive", "gastro", "gastrointestinal", "digestive system"
            - "Urology": recognizes "urinary", "kidney", "urology", "urological"
            - "Oncology": recognizes "cancer", "oncology", "tumor", "chemotherapy"
            - "Pulmonology": recognizes "lungs", "respiratory", "chest", "breathing", "asthma", "copd"
            - IMPORTANT: Always output the FULL specialty name from the list above (the right side), never abbreviations!
            - If user mentions "need surgery" -> specialty = "Surgery" (NOT null, NOT "neuro" or shortened)
            - If unclear/ambiguous, set specialty = null.

            FACILITY / DOCTOR
            - facilityName: only if user names a facility explicitly (exact name or clear partial). Else null.
            - doctorName: only if user names a doctor explicitly (exact name or clear partial). Else null.

            DATE RULES
            - If user gives NO date and NO date range:
            - preferredDateStart = TODAY
            - preferredDateEnd = TODAY + 14 days
            - If user says "today" or "tonight":
            - preferredDateStart = preferredDateEnd = TODAY
            - If user says "tomorrow":
            - preferredDateStart = preferredDateEnd = TODAY + 1 day
            - If user says "asap", "soon", "earliest available":
            - preferredDateStart = TODAY
            - preferredDateEnd = TODAY + 7 days
            - If user says "this week":
            - preferredDateStart = TODAY
            - preferredDateEnd = upcoming Sunday (inclusive)
            - If user says "next week":
            - preferredDateStart = next Monday
            - preferredDateEnd = the following Sunday (inclusive)
            - If user says "this weekend":
            - preferredDateStart = next Saturday
            - preferredDateEnd = next Sunday
            - Weekdays:
            - "this <weekday>" means the next occurrence of that weekday starting from TODAY (could be TODAY if it matches).
            - "next <weekday>" means the occurrence of that weekday in the following week (7–13 days after the "this" occurrence).
            - Set preferredDateStart = preferredDateEnd = computed day.
            - Explicit dates:
            - If user provides one date -> preferredDateStart = preferredDateEnd = that date.
            - If user provides a range like "Feb 15-20" -> fill both.
            - Month/day without year:
            - Use year = 2026 if that date is on/after TODAY; otherwise use year = 2027.
            - If user mentions ONLY time but no date:
            - preferredDateStart = TODAY
            - preferredDateEnd = TODAY + 14 days

            TIME RULES (24-hour output)
            - Always output time as HH:mm (24-hour, zero-padded). Examples: 09:00, 14:30, 19:00.
            - If user gives NO time preference (e.g., "any time", "no preference"):
            - preferredTimeStart = null
            - preferredTimeEnd = null
            - If user gives a part-of-day:
            - "morning" -> 09:00–12:00
            - "afternoon" -> 13:00–17:00
            - "evening" -> 17:00–20:00
            - If user says "after X":
            - preferredTimeStart = X
            - preferredTimeEnd = null
            - If user says "before X":
            - preferredTimeStart = null
            - preferredTimeEnd = X
            - If user gives an exact time "at X":
            - preferredTimeStart = (X - 60 minutes)
            - preferredTimeEnd = (X + 60 minutes)
            - Convert:
            - "noon" -> 12:00
            - "midnight" -> 00:00
            - Clamp clinic hours:
            - Any time bounds earlier than 08:00 become 08:00
            - Any time bounds later than 21:00 become 21:00
            - If after clamping the window is invalid (start >= end), set both time fields to null.

            DURATION RULES
            - If user explicitly gives minutes, map to 15/30/60 only:
            - round to nearest: 1–22 -> 15, 23–44 -> 30, 45+ -> 60
            - If user mentions:
            - "follow-up" or "quick check" -> 15
            - "consultation" or "visit" -> 30
            - "procedure" or "surgery" -> 60
            - If duration not mentioned -> durationMinutes = 30

        """;
    
    private final AgentService agentService;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorLicenseRepository doctorLicenseRepository;
    private final PatientBookingService patientBookingService;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final ObjectMapper objectMapper;
    
    public BookingCopilotService(
        AgentService agentService,
        FacilityRepository facilityRepository,
        SpecialityRepository specialityRepository,
        DoctorRepository doctorRepository,
        DoctorLicenseRepository doctorLicenseRepository,
        PatientBookingService patientBookingService,
        DoctorAvailabilityRepository doctorAvailabilityRepository) {
    this.agentService = agentService;
    this.facilityRepository = facilityRepository;
    this.specialityRepository = specialityRepository;
    this.doctorRepository = doctorRepository;
    this.doctorLicenseRepository = doctorLicenseRepository;
    this.patientBookingService = patientBookingService;
    this.doctorAvailabilityRepository = doctorAvailabilityRepository;
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
            
            // Remove markdown code block wrapper if present
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\n|```\n|```", "").trim();
            }
            
            // Extract JSON from response if it contains extra text
            // Look for { at the beginning and } at the end
            int jsonStart = cleaned.indexOf('{');
            int jsonEnd = cleaned.lastIndexOf('}');
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1).trim();
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
        LocalDate endDate = parseDate(intent.getPreferredDateEnd(), startDate.plusDays(14)).plusDays(7);
        
        BookingIntent relaxed = new BookingIntent();
        relaxed.setSpecialty(intent.getSpecialty());
        relaxed.setFacilityName(intent.getFacilityName());
        relaxed.setDoctorName(intent.getDoctorName());
        relaxed.setPreferredDateStart(startDate.toString());
        relaxed.setPreferredDateEnd(endDate.toString());
        relaxed.setPreferredTimeStart(startTime.toString());
        relaxed.setPreferredTimeEnd(endTime.toString());
        relaxed.setDurationMinutes(intent.getDurationMinutes());
        
        return suggestSlots(relaxed);
    }
    
    private String resolveSpecialty(String userSpecialty) {
        if (userSpecialty == null) return null;
        
        String normalized = userSpecialty.trim().toLowerCase();
        List<Speciality> all = specialityRepository.findAll();
        
        // Try exact match first (case-insensitive)
        for (Speciality s : all) {
            String specName = s.getSpecialityName().toLowerCase();
            if (specName.equals(normalized)) {
                return s.getSpecialityCode();
            }
        }
        
        // Try common abbreviation/keyword mappings
        String mapped = mapCommonSpecialtyNames(normalized);
        if (mapped != null) {
            for (Speciality s : all) {
                String specName = s.getSpecialityName().toLowerCase();
                if (specName.equals(mapped)) {
                    return s.getSpecialityCode();
                }
            }
        }
        
        // Try substring match (user input contained in or contains specialty name)
        for (Speciality s : all) {
            String specName = s.getSpecialityName().toLowerCase();
            if (specName.contains(normalized) || normalized.contains(specName)) {
                return s.getSpecialityCode();
            }
        }
        
        return null;
    }
    
    /**
     * Maps common AI-generated specialty names and abbreviations to full specialty names
     * Handles variations like "neuro" -> "neurology", "cardiac" -> "cardiology", etc.
     * This provides fallback matching in case the AI doesn't return exact names.
     */
    private String mapCommonSpecialtyNames(String input) {
        Map<String, String> specialtyMap = new HashMap<>();
        
        // Neurology variants
        specialtyMap.put("neuro", "neurology");
        specialtyMap.put("neurological", "neurology");
        specialtyMap.put("brain", "neurology");
        
        // Surgery variants
        specialtyMap.put("surgery", "surgery");
        specialtyMap.put("surgical", "surgery");
        specialtyMap.put("surgeon", "surgery");
        
        // Cardiology variants
        specialtyMap.put("cardiac", "cardiology");
        specialtyMap.put("cardio", "cardiology");
        specialtyMap.put("heart", "cardiology");
        
        // Orthopedics variants
        specialtyMap.put("ortho", "orthopedics");
        specialtyMap.put("orthopedic", "orthopedics");
        specialtyMap.put("bones", "orthopedics");
        specialtyMap.put("joint", "orthopedics");
        
        // Dermatology variants
        specialtyMap.put("derma", "dermatology");
        specialtyMap.put("skin", "dermatology");
        
        // Pediatrics variants
        specialtyMap.put("pediatric", "pediatrics");
        specialtyMap.put("peds", "pediatrics");
        specialtyMap.put("children", "pediatrics");
        
        // Psychiatry variants
        specialtyMap.put("psych", "psychiatry");
        specialtyMap.put("mental health", "psychiatry");
        
        // Otolaryngology (ENT) variants
        specialtyMap.put("ent", "otolaryngology");
        specialtyMap.put("ear nose throat", "otolaryngology");
        specialtyMap.put("otorhino", "otolaryngology");
        
        // Gynecology variants
        specialtyMap.put("gynae", "gynecology");
        specialtyMap.put("gynec", "gynecology");
        specialtyMap.put("women", "gynecology");
        
        // Gastroenterology variants
        specialtyMap.put("gastro", "gastroenterology");
        specialtyMap.put("gi", "gastroenterology");
        specialtyMap.put("digestive", "gastroenterology");
        
        // Urology variants
        specialtyMap.put("uro", "urology");
        specialtyMap.put("urinary", "urology");
        
        // Oncology variants
        specialtyMap.put("onco", "oncology");
        specialtyMap.put("cancer", "oncology");
        
        // Pulmonology variants
        specialtyMap.put("pulmo", "pulmonology");
        specialtyMap.put("respiratory", "pulmonology");
        specialtyMap.put("lungs", "pulmonology");
        
        // General Practice variants
        specialtyMap.put("gp", "general practice");
        specialtyMap.put("general", "general practice");
        specialtyMap.put("family medicine", "general practice");
        
        return specialtyMap.get(input);
    }
    
    private Facility resolveFacility(String userFacility) {
        if (userFacility == null) return null;
        
        String normalized = userFacility.trim().toLowerCase();
        List<Facility> all = facilityRepository.findAll();
        
        for (Facility f : all) {
            String fname = f.getName().toLowerCase();
            if (fname.contains(normalized) || normalized.contains(fname)) {
                return f;
            }
        }
        
        return null;
    }
    
    private List<Doctor> findMatchingDoctors(String specialtyCode, UUID facilityId, String doctorName) {
        List<Doctor> candidates = doctorRepository.findAll();
        
        return candidates.stream()
                .filter(d -> matchesSpecialty(d, specialtyCode))
                .filter(d -> matchesFacility(d, facilityId))
                .filter(d -> matchesName(d, doctorName))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private boolean matchesSpecialty(Doctor doctor, String specialtyCode) {
        if (specialtyCode == null) return true;
        List<DoctorLicense> licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(doctor.getId());
        return licenses.stream().anyMatch(lic -> specialtyCode.equals(lic.getSpecialityCode()));
    }
    
    private boolean matchesFacility(Doctor doctor, UUID facilityId) {
        return facilityId == null;
    }
    
    private boolean matchesName(Doctor doctor, String name) {
        if (name == null) return true;
        return doctor.getName().toLowerCase().contains(name.toLowerCase());
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
                        patientBookingService.getAvailabilitySlots(doctor.getId(), facilityId, current, duration, specialtyCode);
                
                for (BookingAvailabilitySlot slot : availability.getSlots()) {
                    if (!"AVAILABLE".equals(slot.getStatus())) continue;
                    
                    LocalTime slotTime = LocalTime.parse(slot.getStartAt().substring(11, 16));
                    if (slotTime.isBefore(startTime) || slotTime.isAfter(endTime)) continue;
                    
                    // Get facility info from the  availability record using the availabilityId
                    String effectiveFacilityId = facilityId != null ? facilityId.toString() : null;
                    String effectiveFacilityName = facilityName;
                    String facilityStateCode = null;
                    
                    if (slot.getAvailabilityId() != null) {
                        try {
                            UUID availId = UUID.fromString(slot.getAvailabilityId());
                            Optional<DoctorAvailability> daOpt = doctorAvailabilityRepository.findById(availId);
                            if (daOpt.isPresent()) {
                                DoctorAvailability da = daOpt.get();
                                if (effectiveFacilityId == null && da.getFacilityId() != null) {
                                    effectiveFacilityId = da.getFacilityId().toString();
                                }
                                if (effectiveFacilityName == null && da.getFacilityId() != null) {
                                    Optional<Facility> facOpt = facilityRepository.findById(da.getFacilityId());
                                    if (facOpt.isPresent()) {
                                        Facility facility = facOpt.get();
                                        effectiveFacilityName = facility.getName();
                                        // Get the facility's state code for license validation
                                        if (facility.getState() != null) {
                                            facilityStateCode = facility.getState().getStateCode();
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Continue without facility info if lookup fails
                        }
                    }
                    
                    // If we found a facility, validate that doctor has license for this speciality in that state
                    if (specialtyCode != null && facilityStateCode != null) {
                        boolean hasLicense = doctorLicenseRepository.existsByDoctorIdAndStateCodeAndSpecialityCodeAndIsActiveTrue(
                                doctor.getId(), facilityStateCode, specialtyCode);
                        if (!hasLicense) {
                            continue; // Skip this slot if doctor doesn't have license in this state for this specialty
                        }
                    }
                    
                    String reason = buildReason(doctor, effectiveFacilityName, specialtyCode, current, slotTime);
                    
                    slots.add(new SlotSuggestion(
                            doctor.getId().toString(),
                            doctor.getName(),
                            getSpecialtyName(specialtyCode),
                            specialtyCode,  
                            effectiveFacilityId,
                            effectiveFacilityName,
                            current.toString(),
                            slot.getStartAt().substring(11, 16),
                            slot.getEndAt().substring(11, 16),
                            duration,
                            reason,
                            slot.getAvailabilityId()
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
