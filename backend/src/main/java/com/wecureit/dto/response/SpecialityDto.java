package com.wecureit.dto.response;

public class SpecialityDto {
    private String code;
    private String name;

    public SpecialityDto() {}

    public SpecialityDto(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
