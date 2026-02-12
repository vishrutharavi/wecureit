package com.wecureit.dto.response;
import lombok.Data;
@Data
public class AgentResponse {
    private String model;
    private String response;
    private boolean done;
}