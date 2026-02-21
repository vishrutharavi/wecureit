package com.wecureit.controller.agents;

import com.wecureit.dto.request.BookingCopilotRequest;
import com.wecureit.dto.response.AgentQuestionResponse;
import com.wecureit.service.AgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;
    
    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }
    
    @PostMapping("/ask")
    public AgentQuestionResponse askQuestion(@RequestBody BookingCopilotRequest request) {
        String answer = agentService.askQuestion(request.getUtterance());
        return new AgentQuestionResponse(request.getUtterance(), answer, "llama3:latest");
    }
    
    @GetMapping("/health")
    public String health() {
        return "Agent service is running!";
    }
}