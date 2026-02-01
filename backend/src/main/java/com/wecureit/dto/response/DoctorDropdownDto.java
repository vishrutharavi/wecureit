package com.wecureit.dto.response;

import java.util.List;
import java.util.UUID;

public class DoctorDropdownDto {
    private UUID id;
    private String displayName;
    private String title;
    private List<SpecialityDto> specialties;
    private List<FacilityRef> facilities;

    public static class FacilityRef {
        public UUID id;
        public String name;
        public FacilityRef() {}
        public FacilityRef(UUID id, String name) { this.id = id; this.name = name; }
    }

    public DoctorDropdownDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<SpecialityDto> getSpecialties() { return specialties; }
    public void setSpecialties(List<SpecialityDto> specialties) { this.specialties = specialties; }

    public List<FacilityRef> getFacilities() { return facilities; }
    public void setFacilities(List<FacilityRef> facilities) { this.facilities = facilities; }
}
