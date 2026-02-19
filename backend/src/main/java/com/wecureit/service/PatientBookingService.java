package com.wecureit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wecureit.dto.response.BookingDropdownResponse;
import com.wecureit.dto.response.DoctorDropdownDto;
import com.wecureit.dto.response.SpecialityResponse;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.DoctorLicense;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Speciality;
import com.wecureit.repository.DoctorFacilityLockRepository;
import com.wecureit.repository.DoctorLicenseRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.FacilityRepository;
import com.wecureit.repository.RoomRepository;
import com.wecureit.repository.SpecialityRepository;

@Service
public class PatientBookingService {

    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final SpecialityRepository specialityRepository;
    private final RoomRepository roomRepository;
    private final DoctorLicenseRepository doctorLicenseRepository;
    private final com.wecureit.repository.AppointmentRepository appointmentRepository;
    private final com.wecureit.service.DoctorAvailabilityService doctorAvailabilityService;
    private final DoctorFacilityLockRepository doctorFacilityLockRepository;
    private final DoctorFacilityService doctorFacilityService;

    public PatientBookingService(DoctorRepository doctorRepository,
                                 FacilityRepository facilityRepository,
                                 SpecialityRepository specialityRepository,
                                 RoomRepository roomRepository,
                                 DoctorLicenseRepository doctorLicenseRepository,
                                 com.wecureit.repository.AppointmentRepository appointmentRepository,
                                 com.wecureit.service.DoctorAvailabilityService doctorAvailabilityService,
                                 DoctorFacilityLockRepository doctorFacilityLockRepository,
                                 DoctorFacilityService doctorFacilityService) {
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
        this.roomRepository = roomRepository;
        this.doctorLicenseRepository = doctorLicenseRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorAvailabilityService = doctorAvailabilityService;
        this.doctorFacilityLockRepository = doctorFacilityLockRepository;
        this.doctorFacilityService = doctorFacilityService;
    }

    /**
     * Returns 15-minute slots for a doctor on a given workDate (YYYY-MM-DD).
     * Status values: AVAILABLE | BOOKED | BREAK_ENFORCED | UNAVAILABLE
     */
    public com.wecureit.dto.response.BookingAvailabilityResponse getAvailabilitySlots(java.util.UUID doctorId, java.util.UUID facilityId, java.time.LocalDate workDate, Integer desiredDurationMinutes) {
        return getAvailabilitySlots(doctorId, facilityId, workDate, desiredDurationMinutes, null);
    }

    public com.wecureit.dto.response.BookingAvailabilityResponse getAvailabilitySlots(java.util.UUID doctorId, java.util.UUID facilityId, java.time.LocalDate workDate, Integer desiredDurationMinutes, String specialityCode) {
        var out = new com.wecureit.dto.response.BookingAvailabilityResponse();
        out.setDoctorId(doctorId);
        out.setFacilityId(facilityId);
        out.setWorkDate(workDate.toString());

        // fetch availability windows for this doctor on the date
        java.util.List<com.wecureit.dto.response.AvailabilityResponse> windows = doctorAvailabilityService.listAvailabilities(doctorId, workDate, workDate);

        // fetch appointments for the doctor for the full day
        java.time.LocalDateTime startAt = workDate.atStartOfDay();
        java.time.LocalDateTime endAt = workDate.plusDays(1).atStartOfDay();
        java.util.List<com.wecureit.entity.Appointment> appts = appointmentRepository.findAppointmentsForDoctor(doctorId, startAt, endAt);
        if (appts == null) appts = new java.util.ArrayList<>();

        java.util.List<com.wecureit.dto.response.BookingAvailabilitySlot> slots = new java.util.ArrayList<>();
        final int SLOT_MIN = 15;

        for (var w : windows) {
            // if facilityId is provided and doesn't match, skip this window
            if (facilityId != null && w.getFacilityId() != null && !facilityId.toString().equals(w.getFacilityId())) continue;
            if (!w.isBookable()) continue;
            // parse start/end time (HH:MM)
            try {
                var stParts = w.getStartTime().split(":");
                var enParts = w.getEndTime().split(":");
                int stMin = Integer.parseInt(stParts[0]) * 60 + Integer.parseInt(stParts[1]);
                int enMin = Integer.parseInt(enParts[0]) * 60 + Integer.parseInt(enParts[1]);
                for (int t = stMin; t + SLOT_MIN <= enMin; t += SLOT_MIN) {
                    java.time.LocalDateTime sdt = workDate.atStartOfDay().plusMinutes(t);
                    java.time.LocalDateTime edt = sdt.plusMinutes(SLOT_MIN);
                    String status = "AVAILABLE";
                    // check appointment overlap — use the full desired duration end time so that slots
                    // whose full-duration window overlaps an existing appointment are marked BOOKED
                    // (not just UNAVAILABLE via break simulation).
                    java.time.LocalDateTime fullEndDt = (desiredDurationMinutes != null && desiredDurationMinutes > SLOT_MIN)
                            ? sdt.plusMinutes(desiredDurationMinutes) : edt;
                    boolean overlapped = false;
                    for (var a : appts) {
                        if (a.getIsActive() != null && !a.getIsActive()) continue;
                        if (a.getStartTime() == null || a.getEndTime() == null) continue;
                        // when filtering by speciality, only mark slots as BOOKED for same-speciality appointments
                        if (specialityCode != null && a.getSpeciality() != null &&
                            !specialityCode.equals(a.getSpeciality().getSpecialityCode())) continue;
                        if (a.getStartTime().isBefore(fullEndDt) && a.getEndTime().isAfter(sdt)) { overlapped = true; break; }
                    }
                    if (overlapped) status = "BOOKED";
                    else {
                        // Previously we read persisted break_* columns to enforce break slots. Those columns
                        // have been removed from the schema; fall back to simulation-only logic below.
                        if (desiredDurationMinutes != null && desiredDurationMinutes > 0) {
                            // build a simulated list of appointments including a hypothetical appointment starting at sdt
                            java.util.List<com.wecureit.entity.Appointment> sim = new java.util.ArrayList<>();
                            // only include active appointments that have start/end
                            for (var a : appts) {
                                if (a.getIsActive() == null || a.getIsActive()) {
                                    if (a.getStartTime() != null && a.getEndTime() != null) sim.add(a);
                                }
                            }
                            // create a lightweight hypothetical appointment object
                            com.wecureit.entity.Appointment hypo = new com.wecureit.entity.Appointment();
                            hypo.setStartTime(sdt);
                            hypo.setEndTime(sdt.plusMinutes(desiredDurationMinutes));
                            hypo.setDuration((long) desiredDurationMinutes);
                            sim.add(hypo);

                            // sort by startTime
                            sim.sort((a1, a2) -> a1.getStartTime().compareTo(a2.getStartTime()));

                            // scan contiguous groups (gap < GAP_MIN minutes considered continuous) and sum durations
                            // IMPORTANT: only consider a violation if the contiguous group that includes the hypothetical
                            // booking reaches or exceeds the maximum continuous minutes. Previously any group anywhere in the day
                            // could trigger the violation which incorrectly made all simulated slots unavailable once a 60-minute
                            // appointment existed.
                            final long GAP_MIN = 15L;
                            final long MAX_CONTINUOUS_MINUTES = 60L;
                            long cum = 0L;
                            java.time.LocalDateTime prev = null;
                            boolean violation = false;
                            boolean groupHasHypo = false;
                            for (var sa : sim) {
                                long dur = sa.getDuration() == null ? java.time.Duration.between(sa.getStartTime(), sa.getEndTime()).toMinutes() : sa.getDuration().longValue();
                                boolean isHypo = sa == hypo;
                                if (prev == null) {
                                    cum = dur;
                                    groupHasHypo = isHypo;
                                } else {
                                    long gap = java.time.Duration.between(prev, sa.getStartTime()).toMinutes();
                                    // treat a 15-minute gap as a valid break boundary. Only gaps strictly less than GAP_MIN are continuous.
                                    if (gap < GAP_MIN) {
                                        cum += dur;
                                        groupHasHypo = groupHasHypo || isHypo;
                                    } else {
                                        cum = dur;
                                        groupHasHypo = isHypo;
                                    }
                                }
                                // only treat as a violation when the contiguous group that includes the hypothetical booking
                                // would strictly exceed the limit (the policy: breaks are required only when continuous
                                // work would be more than 60 minutes; exactly 60 is allowed without an extra break).
                                if (groupHasHypo && cum > MAX_CONTINUOUS_MINUTES) { violation = true; break; }
                                prev = sa.getEndTime();
                            }
                            if (violation) status = "UNAVAILABLE";
                        }
                    }

                    // per-slot room availability check: if no rooms available for the speciality at this slot, mark UNAVAILABLE
                    if (facilityId != null && "AVAILABLE".equals(status)) {
                        String slotSpecCode = specialityCode != null ? specialityCode : w.getSpecialityCode();
                        if (slotSpecCode != null) {
                            com.wecureit.entity.Room availRoom = doctorFacilityService.findAvailableRoom(facilityId, slotSpecCode, sdt, edt);
                            if (availRoom == null) {
                                status = "UNAVAILABLE";
                            }
                        }
                    }

                    // include the originating availability window id when available so frontend can persist it
                    String availId = null;
                    try { if (w.getId() != null) availId = w.getId().toString(); } catch (Exception e) { availId = null; }
                    slots.add(new com.wecureit.dto.response.BookingAvailabilitySlot(sdt.toString(), edt.toString(), status, availId));
                }
            } catch (Exception ex) {
                // ignore malformed window
            }
        }

        // Deduplicate slots by startAt time - keep first occurrence but prefer more restrictive status
        java.util.Map<String, com.wecureit.dto.response.BookingAvailabilitySlot> uniqueSlots = new java.util.LinkedHashMap<>();
        for (var slot : slots) {
            String key = slot.getStartAt();
            if (!uniqueSlots.containsKey(key)) {
                uniqueSlots.put(key, slot);
            } else {
                // If duplicate exists, prefer BOOKED/UNAVAILABLE over AVAILABLE (more restrictive wins)
                var existing = uniqueSlots.get(key);
                if ("AVAILABLE".equals(existing.getStatus()) && 
                    ("BOOKED".equals(slot.getStatus()) || "UNAVAILABLE".equals(slot.getStatus()) || "BREAK_ENFORCED".equals(slot.getStatus()))) {
                    uniqueSlots.put(key, slot);
                }
            }
        }
        
        // Convert back to list and sort by time
        java.util.List<com.wecureit.dto.response.BookingAvailabilitySlot> dedupedSlots = 
            new java.util.ArrayList<>(uniqueSlots.values());
        dedupedSlots.sort((a, b) -> a.getStartAt().compareTo(b.getStartAt()));

        out.setSlots(dedupedSlots);
        return out;
    }

    public BookingDropdownResponse getDropdownData(UUID facilityId, String specialityCode, UUID doctorId, java.time.LocalDate workDate) {
        BookingDropdownResponse out = new BookingDropdownResponse();

        // specialities
        List<Speciality> specs = specialityRepository.findAll();
        List<SpecialityResponse> specDtos = specs.stream().map(s -> new SpecialityResponse(s.getSpecialityCode(), s.getSpecialityName())).collect(Collectors.toList());
        out.setSpecialties(specDtos);

        // facilities (active)
        List<Facility> facilities = facilityRepository.findAllByIsActive(Boolean.TRUE);
        List<com.wecureit.dto.response.FacilityResponse> facDtos = new ArrayList<>();
        for (Facility f : facilities) {
            // if speciality filter is provided, ensure facility has at least one active room for that speciality
            if (specialityCode != null && !specialityCode.isBlank()) {
                var rooms = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(f.getId(), specialityCode);
                if (rooms == null || rooms.isEmpty()) continue;
            }
            com.wecureit.dto.response.FacilityResponse fd = new com.wecureit.dto.response.FacilityResponse(f.getId(), f.getName(), f.getCity(), f.getState() == null ? null : f.getState().getStateCode(), f.getIsActive(), f.getAddress(), f.getZipCode());
            // attach specialties available at facility (based on rooms)
            List<com.wecureit.entity.Room> rooms = roomRepository.findByFacilityIdAndActiveTrue(f.getId());
            List<SpecialityResponse> fs = rooms.stream()
                .map(r -> {
                    String code = r.getSpecialityCode();
                    String name = code;
                    if (code != null) {
                        var maybe = specialityRepository.findById(code);
                        if (maybe.isPresent()) name = maybe.get().getSpecialityName();
                    }
                    return new SpecialityResponse(code, name);
                })
                .filter(s -> s.getCode() != null)
                .collect(Collectors.groupingBy(s -> s.getCode()))
                .values().stream().map(l -> l.get(0)).collect(Collectors.toList());
            fd.setSpecialties(fs);
            facDtos.add(fd);
        }
        // If doctor+workDate provided and a facility lock exists, restrict facilities to the locked facility
        if (doctorId != null && workDate != null) {
            var lockOpt = doctorFacilityLockRepository.findByDoctorIdAndWorkDate(doctorId, workDate);
            if (lockOpt.isPresent()) {
                var lock = lockOpt.get();
                var maybeFac = facDtos.stream().filter(fd -> fd.getId().equals(lock.getFacilityId())).findFirst();
                if (maybeFac.isPresent()) {
                    facDtos = new ArrayList<>();
                    facDtos.add(maybeFac.get());
                } else {
                    // locked facility is not in current list (maybe inactive or filtered out by speciality) -> fetch it explicitly
                    var facEntityOpt = facilityRepository.findById(lock.getFacilityId());
                    if (facEntityOpt.isPresent()) {
                        var f = facEntityOpt.get();
                        com.wecureit.dto.response.FacilityResponse fd = new com.wecureit.dto.response.FacilityResponse(f.getId(), f.getName(), f.getCity(), f.getState() == null ? null : f.getState().getStateCode(), f.getIsActive(), f.getAddress(), f.getZipCode());
                        // attach specialties
                        List<com.wecureit.entity.Room> rooms = roomRepository.findByFacilityIdAndActiveTrue(f.getId());
                        List<SpecialityResponse> fs = rooms.stream()
                            .map(r -> {
                                String code = r.getSpecialityCode();
                                String name = code;
                                if (code != null) {
                                    var maybe = specialityRepository.findById(code);
                                    if (maybe.isPresent()) name = maybe.get().getSpecialityName();
                                }
                                return new SpecialityResponse(code, name);
                            })
                            .filter(s -> s.getCode() != null)
                            .collect(Collectors.groupingBy(s -> s.getCode()))
                            .values().stream().map(l -> l.get(0)).collect(Collectors.toList());
                        fd.setSpecialties(fs);
                        facDtos = new ArrayList<>();
                        facDtos.add(fd);
                    }
                }
            }
        }

        out.setFacilities(facDtos);

        // doctors (apply filters and optionally doctorId)
        List<Doctor> doctors = doctorRepository.findByIsActiveTrue();
        List<DoctorDropdownDto> docDtos = new ArrayList<>();
        // precompute selected facility state (if provided) to make specialty checks state-aware
        final String selectedFacilityState;
        if (facilityId != null) {
            var maybeFac = facDtos.stream().filter(fd -> fd.getId().equals(facilityId)).findFirst();
            selectedFacilityState = maybeFac.map(fx -> fx.getState()).orElse(null);
        } else {
            selectedFacilityState = null;
        }
        for (Doctor d : doctors) {
            // if doctorId is provided and doesn't match, skip
            if (doctorId != null && !doctorId.equals(d.getId())) continue;

            DoctorDropdownDto dd = new DoctorDropdownDto();
            dd.setId(d.getId());
            dd.setDisplayName(d.getName());
            dd.setTitle(null);

            // find active licenses for doctor
            List<DoctorLicense> licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(d.getId());
            List<SpecialityResponse> sdocs = licenses.stream().map(l -> {
                String code = l.getSpecialityCode();
                String name = code;
                if (code != null) {
                    var maybe = specialityRepository.findById(code);
                    if (maybe.isPresent()) name = maybe.get().getSpecialityName();
                }
                return new SpecialityResponse(code, name);
            }).collect(Collectors.toList());
            dd.setSpecialties(sdocs);

            // determine facilities for this doctor (facilities where doctor's speciality rooms exist in same state)
            List<com.wecureit.dto.response.FacilityResponse> matched = facDtos.stream().filter(fd -> {
                // facility state
                String state = fd.getState();
                // check doctor licenses in same state
                for (DoctorLicense lic : licenses) {
                    if (lic.getStateCode() != null && lic.getStateCode().equals(state)) {
                        // ensure facility offers that speciality
                        List<com.wecureit.entity.Room> rs = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(fd.getId(), lic.getSpecialityCode());
                        if (rs != null && !rs.isEmpty()) return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());

            List<DoctorDropdownDto.FacilityRef> facRefs = matched.stream().map(m -> new DoctorDropdownDto.FacilityRef(m.getId(), m.getName())).collect(Collectors.toList());
            dd.setFacilities(facRefs);

            // apply top-level filters if provided
            if (facilityId != null) {
                boolean worksAt = facRefs.stream().anyMatch(fr -> fr.id.equals(facilityId));
                if (!worksAt) continue;
            }
            if (specialityCode != null && !specialityCode.isBlank()) {
                boolean hasSpec = false;
                if (selectedFacilityState != null) {
                    // require that the doctor has an active license for this speciality in the facility's state
                    List<DoctorLicense> licensesForDoctor = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(d.getId());
                    hasSpec = licensesForDoctor.stream().anyMatch(l -> l.getSpecialityCode() != null && l.getSpecialityCode().equalsIgnoreCase(specialityCode) && l.getStateCode() != null && l.getStateCode().equals(selectedFacilityState));
                } else {
                    // no facility selected -> any active license counts
                    hasSpec = sdocs.stream().anyMatch(s -> specialityCode.equalsIgnoreCase(s.getCode()));
                }
                if (!hasSpec) continue;
            }

            docDtos.add(dd);
        }
        out.setDoctors(docDtos);

        // Final cross-filtering: compute authoritative lists depending on provided params
        List<com.wecureit.dto.response.FacilityResponse> finalFacs = new ArrayList<>(facDtos);
        List<DoctorDropdownDto> finalDocs = new ArrayList<>(docDtos);
        List<SpecialityResponse> finalSpecs = new ArrayList<>(specDtos);

        // If doctorId provided, restrict facilities to those the doctor works at and specialties to that doctor's specialties
        if (doctorId != null) {
            var matchDocOpt = docDtos.stream().filter(d -> d.getId().equals(doctorId)).findFirst();
            if (matchDocOpt.isPresent()) {
                DoctorDropdownDto dd = matchDocOpt.get();
                var facIdSet = dd.getFacilities().stream().map(fr -> fr.id).collect(Collectors.toSet());
                finalFacs = facDtos.stream().filter(f -> facIdSet.contains(f.getId())).collect(Collectors.toList());

                // doctor's specialties
                // Recompute doctor's licensed specialties with state information so we can intersect with facility state
                List<DoctorLicense> licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(doctorId);
                var docSpecCodes = licenses.stream().map(l -> l.getSpecialityCode()).collect(Collectors.toSet());

                // if facilityId also provided, further intersect with facility-supported specialties
                if (facilityId != null) {
                    finalFacs = finalFacs.stream().filter(f -> f.getId().equals(facilityId)).collect(Collectors.toList());
                    // gather specialties supported by this facility (rooms)
                    var facSpecCodes = finalFacs.stream().flatMap(f -> f.getSpecialties().stream()).map(SpecialityResponse::getCode).collect(Collectors.toSet());
                    // Additionally, restrict doctor's specialties to those licensed in the facility's state
                    if (!finalFacs.isEmpty()) {
                        String facState = finalFacs.get(0).getState();
                        var licensedInState = licenses.stream()
                            .filter(l -> l.getStateCode() != null && l.getStateCode().equals(facState))
                            .map(l -> l.getSpecialityCode())
                            .collect(Collectors.toSet());
                        docSpecCodes.retainAll(licensedInState);
                    }
                    docSpecCodes.retainAll(facSpecCodes);
                }

                finalSpecs = specDtos.stream().filter(s -> s.getCode() != null && docSpecCodes.contains(s.getCode())).collect(Collectors.toList());
            } else {
                // doctorId provided but not found in filtered doctors -> empty results
                finalDocs = new ArrayList<>();
                finalFacs = new ArrayList<>();
                finalSpecs = new ArrayList<>();
            }
            // If a speciality filter was provided, ensure finalFacs only includes facilities that support that speciality
            if (specialityCode != null && !specialityCode.isBlank() && !finalFacs.isEmpty()) {
                finalFacs = finalFacs.stream().filter(f -> {
                    // facility must support the speciality (rooms) and the doctor must be licensed for that speciality in the facility state
                    var rooms = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(f.getId(), specialityCode);
                    if (rooms == null || rooms.isEmpty()) return false;
                    // doctor must have a license for this speciality in this facility's state
                    var licenses = doctorLicenseRepository.findByDoctorIdAndIsActiveTrue(doctorId);
                    boolean licensedHere = licenses.stream().anyMatch(l -> l.getSpecialityCode() != null && l.getSpecialityCode().equalsIgnoreCase(specialityCode) && l.getStateCode() != null && l.getStateCode().equals(f.getState()));
                    return licensedHere;
                }).collect(Collectors.toList());
                // if after filtering no facilities remain, clear doctors and specialties as well to indicate no matches
                // If no facilities remain after filtering, do not wipe doctors/specialties here.
                // The frontend should display zero options while preserving the current selections
                // so users can understand there are no matches without losing context.
            }
        } else {
            // No specific doctor provided
            if (facilityId != null) {
                // restrict facilities list to the selected facility (if present)
                finalFacs = facDtos.stream().filter(f -> f.getId().equals(facilityId)).collect(Collectors.toList());
                // specialties supported by that facility
                var facSpecCodes = finalFacs.stream().flatMap(f -> f.getSpecialties().stream()).map(SpecialityResponse::getCode).collect(Collectors.toSet());
                finalSpecs = specDtos.stream().filter(s -> s.getCode() != null && facSpecCodes.contains(s.getCode())).collect(Collectors.toList());

                // if specialityCode also provided, ensure specialties list is that code only (if supported)
                if (specialityCode != null && !specialityCode.isBlank()) {
                        finalSpecs = finalSpecs.stream().filter(s -> specialityCode.equalsIgnoreCase(s.getCode())).collect(Collectors.toList());
                }
            } else if (specialityCode != null && !specialityCode.isBlank()) {
                // no facility or doctor, but specialty filter provided -> only include that specialty in list
                finalSpecs = specDtos.stream().filter(s -> specialityCode.equalsIgnoreCase(s.getCode())).collect(Collectors.toList());
            }
        }

        out.setFacilities(finalFacs);
        out.setDoctors(finalDocs);
        out.setSpecialties(finalSpecs);

        return out;
    }
}
