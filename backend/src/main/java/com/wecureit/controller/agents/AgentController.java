package com.wecureit.controller.agents;

import com.wecureit.dto.response.AgentQuestionResponse;
import com.wecureit.service.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }
    @GetMapping("/ask")
    public AgentQuestionResponse askQuestion(@RequestParam String question) {
        String answer = agentService.askQuestion(question);
        return new AgentQuestionResponse(question, answer, "llama3:latest");
    }
    @GetMapping("/health")
    public String health() {
        return "Service is running!";
    }
}