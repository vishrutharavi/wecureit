package com.wecureit.dto.response;

public class StateResponse {

    private String code;
    private String name;

    public StateResponse(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
}

