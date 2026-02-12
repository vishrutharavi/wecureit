package com.wecureit.service;
import com.wecureit.dto.request.AgentRequest;
import com.wecureit.dto.response.AgentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
@Service
public class AgentService {
    private final WebClient webClient;
    private static final String AGENT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3:latest";
    public AgentService() {
        this.webClient = WebClient.builder()
                .baseUrl(AGENT_BASE_URL)
                .build();
    }
    public String askQuestion(String question) {
        AgentRequest request = new AgentRequest(DEFAULT_MODEL, question, false);
        try {
            AgentResponse response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AgentResponse.class)
                    .block();
            return response != null ? response.getResponse() : "No response from Agent";
        } catch (Exception e) {
            return "Error: " + e.getMessage() + 
                   ". Make sure Agent is running and model is downloaded.";
        }
    }
}