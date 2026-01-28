package com.wecureit.dto.response;

import java.util.List;
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
}

