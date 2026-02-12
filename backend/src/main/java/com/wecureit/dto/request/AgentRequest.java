package com.wecureit.dto.request;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    private String model;
    private String prompt;
    private boolean stream;
}
