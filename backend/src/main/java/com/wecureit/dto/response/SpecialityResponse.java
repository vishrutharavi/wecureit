package com.wecureit.dto.response;

public class SpecialityResponse {

    private String code;
    private String name;

    public SpecialityResponse(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
}
