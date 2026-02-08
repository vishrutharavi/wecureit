package com.wecureit.dto.response;

import java.util.List;
import java.util.UUID;

public class FacilityDropdownDto {
    private UUID id;
    private String name;
    private String city;
    private String state;
    private String address;
    private List<SpecialityResponse> specialties;

    public FacilityDropdownDto() {}

    public FacilityDropdownDto(UUID id, String name, String city, String state, String address) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.state = state;
        this.address = address;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<SpecialityResponse> getSpecialties() { return specialties; }
    public void setSpecialties(List<SpecialityResponse> specialties) { this.specialties = specialties; }
}
