package com.wecureit.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public class UpdateFacilityRequest {

    @NotBlank
    public String name;

    public String address;

    public String city;

    public String stateCode;

    @JsonAlias({"zip", "zipCode"})
    public String zipCode;

}
