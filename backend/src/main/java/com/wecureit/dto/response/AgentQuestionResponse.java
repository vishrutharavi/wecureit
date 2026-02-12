package com.wecureit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class AgentQuestionResponse {
    private String question;
    private String answer;
    private String model;
}
