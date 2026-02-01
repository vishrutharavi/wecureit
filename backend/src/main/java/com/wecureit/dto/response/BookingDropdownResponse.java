package com.wecureit.dto.response;

import java.util.List;

public class BookingDropdownResponse {
    private List<DoctorDropdownDto> doctors;
    private List<FacilityDropdownDto> facilities;
    private List<SpecialityDto> specialties;

    public BookingDropdownResponse() {}

    public List<DoctorDropdownDto> getDoctors() { return doctors; }
    public void setDoctors(List<DoctorDropdownDto> doctors) { this.doctors = doctors; }

    public List<FacilityDropdownDto> getFacilities() { return facilities; }
    public void setFacilities(List<FacilityDropdownDto> facilities) { this.facilities = facilities; }

    public List<SpecialityDto> getSpecialties() { return specialties; }
    public void setSpecialties(List<SpecialityDto> specialties) { this.specialties = specialties; }
}
