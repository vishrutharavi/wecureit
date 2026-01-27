package com.wecureit.dto.response;

import java.util.UUID;

public class FacilityResponse {

    public UUID id;
    public String name;
    public String city;
    public String state;
    public Boolean active;

    public FacilityResponse(UUID id, String name, String city, String state, Boolean active) 
    {
        this.id = id;
        this.name = name;
        this.city = city;
        this.state = state;
        this.active = active;
    }
}

