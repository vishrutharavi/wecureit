package com.wecureit.dto.response;

import java.util.List;
import com.wecureit.dto.response.FacilityResponse;

public class BookingDropdownResponse {
    private List<DoctorDropdownDto> doctors;
    private List<FacilityResponse> facilities;
    private List<SpecialityResponse> specialties;

    public BookingDropdownResponse() {}

    public List<DoctorDropdownDto> getDoctors() { return doctors; }
    public void setDoctors(List<DoctorDropdownDto> doctors) { this.doctors = doctors; }

    public List<FacilityResponse> getFacilities() { return facilities; }
    public void setFacilities(List<FacilityResponse> facilities) { this.facilities = facilities; }

    public List<SpecialityResponse> getSpecialties() { return specialties; }
    public void setSpecialties(List<SpecialityResponse> specialties) { this.specialties = specialties; }
}
