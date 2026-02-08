package com.wecureit.dto.response;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FacilityResponse {

    public UUID id;
    public String name;
    public String city;
    public String state;
    public Boolean active;
    public String address;
    public String zipCode;
    public List<RoomResponse> rooms;

    public FacilityResponse(UUID id, String name, String city, String state, Boolean active, String address, String zipCode) 
    {
        this.id = id;
        this.name = name;
        this.city = city;
        this.state = state;
        this.active = active;
        this.address = address;
        this.zipCode = zipCode;
    }

    public FacilityResponse(UUID id, String name, String city, String state, Boolean active, String address, String zipCode, List<RoomResponse> rooms) {
        this(id, name, city, state, active, address, zipCode);
        this.rooms = rooms;
    }

    // Optional list of specialties for dropdown-like usages
    private List<SpecialityResponse> specialties;

    // Getters and setters added to be compatible where FacilityResponse is used like FacilityDropdownDto
    public UUID getId() { return this.id; }
    public String getName() { return this.name; }
    public String getCity() { return this.city; }
    public String getState() { return this.state; }
    public Boolean getActive() { return this.active; }
    public String getAddress() { return this.address; }
    public String getZipCode() { return this.zipCode; }

    public List<SpecialityResponse> getSpecialties() { return this.specialties; }
    public void setSpecialties(List<SpecialityResponse> specialties) { this.specialties = specialties; }

    // Also provide setters for completeness
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setActive(Boolean active) { this.active = active; }
    public void setAddress(String address) { this.address = address; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacilityResponse that = (FacilityResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

