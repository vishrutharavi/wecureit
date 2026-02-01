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
                boolean hasSpec = sdocs.stream().anyMatch(s -> specialityCode.equalsIgnoreCase(s.getCode()));
                if (!hasSpec) continue;
            }

            docDtos.add(dd);
        }
        out.setDoctors(docDtos);

        // If a specific doctorId was requested, and that doctor exists, we may want to further restrict facilities and specialties
        if (doctorId != null) {
            // find the doctor DTO
            var matchDoc = docDtos.stream().findFirst();
            if (matchDoc.isPresent()) {
                DoctorDropdownDto dd = matchDoc.get();
                // facilities: only facilities where this doctor works
                var facIdSet = dd.getFacilities().stream().map(fr -> fr.id).collect(Collectors.toSet());
                List<FacilityDropdownDto> filteredFacs = facDtos.stream().filter(f -> facIdSet.contains(f.getId())).collect(Collectors.toList());
                out.setFacilities(filteredFacs);

                // specialties: only specialties that doctor has (from dd.specialties) intersected with facility support
                List<SpecialityDto> docSpecs = dd.getSpecialties();
                // ensure uniqueness
                var uniq = docSpecs.stream().collect(Collectors.groupingBy(SpecialityDto::getCode)).values().stream().map(l -> l.get(0)).collect(Collectors.toList());
                out.setSpecialties(uniq);
            }
        }

        return out;
    }
}
