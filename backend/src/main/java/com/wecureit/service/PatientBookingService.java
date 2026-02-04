package com.wecureit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wecureit.dto.response.BookingDropdownResponse;
import com.wecureit.dto.response.DoctorDropdownDto;
import com.wecureit.dto.response.FacilityDropdownDto;
import com.wecureit.dto.response.SpecialityDto;
import com.wecureit.entity.Facility;
import com.wecureit.entity.Speciality;
import com.wecureit.entity.Doctor;
import com.wecureit.entity.DoctorLicense;
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

    public PatientBookingService(DoctorRepository doctorRepository,
                                 FacilityRepository facilityRepository,
                                 SpecialityRepository specialityRepository,
                                 RoomRepository roomRepository,
                                 DoctorLicenseRepository doctorLicenseRepository) {
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.specialityRepository = specialityRepository;
        this.roomRepository = roomRepository;
        this.doctorLicenseRepository = doctorLicenseRepository;
    }

    public BookingDropdownResponse getDropdownData(UUID facilityId, String specialityCode, UUID doctorId) {
        BookingDropdownResponse out = new BookingDropdownResponse();

        // specialities
        List<Speciality> specs = specialityRepository.findAll();
        List<SpecialityDto> specDtos = specs.stream().map(s -> new SpecialityDto(s.getSpecialityCode(), s.getSpecialityName())).collect(Collectors.toList());
        out.setSpecialties(specDtos);

        // facilities (active)
        List<Facility> facilities = facilityRepository.findAllByIsActive(Boolean.TRUE);
        List<FacilityDropdownDto> facDtos = new ArrayList<>();
        for (Facility f : facilities) {
            // if speciality filter is provided, ensure facility has at least one active room for that speciality
            if (specialityCode != null && !specialityCode.isBlank()) {
                var rooms = roomRepository.findByFacilityIdAndSpecialityCodeAndActiveTrue(f.getId(), specialityCode);
                if (rooms == null || rooms.isEmpty()) continue;
            }
            FacilityDropdownDto fd = new FacilityDropdownDto(f.getId(), f.getName(), f.getCity(), f.getState() == null ? null : f.getState().getStateCode(), f.getAddress());
            // attach specialties available at facility (based on rooms)
            List<com.wecureit.entity.Room> rooms = roomRepository.findByFacilityIdAndActiveTrue(f.getId());
            List<SpecialityDto> fs = rooms.stream()
                .map(r -> {
                    String code = r.getSpecialityCode();
                    String name = code;
                    if (code != null) {
                        var maybe = specialityRepository.findById(code);
                        if (maybe.isPresent()) name = maybe.get().getSpecialityName();
                    }
                    return new SpecialityDto(code, name);
                })
                .filter(s -> s.getCode() != null)
                .collect(Collectors.groupingBy(s -> s.getCode()))
                .values().stream().map(l -> l.get(0)).collect(Collectors.toList());
            fd.setSpecialties(fs);
            facDtos.add(fd);
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
            List<SpecialityDto> sdocs = licenses.stream().map(l -> {
                String code = l.getSpecialityCode();
                String name = code;
                if (code != null) {
                    var maybe = specialityRepository.findById(code);
                    if (maybe.isPresent()) name = maybe.get().getSpecialityName();
                }
                return new SpecialityDto(code, name);
            }).collect(Collectors.toList());
            dd.setSpecialties(sdocs);

            // determine facilities for this doctor (facilities where doctor's speciality rooms exist in same state)
            List<FacilityDropdownDto> matched = facDtos.stream().filter(fd -> {
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
        List<FacilityDropdownDto> finalFacs = new ArrayList<>(facDtos);
        List<DoctorDropdownDto> finalDocs = new ArrayList<>(docDtos);
        List<SpecialityDto> finalSpecs = new ArrayList<>(specDtos);

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
                    var facSpecCodes = finalFacs.stream().flatMap(f -> f.getSpecialties().stream()).map(SpecialityDto::getCode).collect(Collectors.toSet());
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
                var facSpecCodes = finalFacs.stream().flatMap(f -> f.getSpecialties().stream()).map(SpecialityDto::getCode).collect(Collectors.toSet());
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
