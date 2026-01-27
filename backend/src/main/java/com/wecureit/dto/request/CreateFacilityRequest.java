package com.wecureit.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;

public class CreateFacilityRequest {

    @NotBlank
    public String name;

    @NotBlank
    public String address;

    @NotBlank
    public String city;

    @NotBlank
    public String stateCode;

    @NotBlank
    @JsonAlias({"zip", "zipCode"})
    public String zipCode;

}
