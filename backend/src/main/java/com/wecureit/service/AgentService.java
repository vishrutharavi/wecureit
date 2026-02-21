package com.wecureit.service;

import com.wecureit.dto.request.AgentRequest;
import com.wecureit.dto.request.OllamaChatRequest;
import com.wecureit.dto.response.AgentResponse;
import com.wecureit.dto.response.OllamaChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
public class AgentService {
    private final WebClient webClient;
    private final String defaultModel;
    private final int timeoutSeconds;

    public AgentService(
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model,
            @Value("${ollama.timeout-seconds:30}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.defaultModel = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String askQuestion(String question) {
        AgentRequest request = new AgentRequest(defaultModel, question, false);
        try {
            AgentResponse response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AgentResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            return response != null ? response.getResponse() : "No response from Agent";
        } catch (Exception e) {
            return "Error: " + e.getMessage() + 
                   ". Make sure Ollama is running and model is downloaded.";
        }
    }

    public String chatWithSystemPrompt(String systemPrompt, String userPrompt) {
        OllamaChatRequest request = new OllamaChatRequest();
        request.setModel(defaultModel);
        request.setStream(false);
        request.setMessages(List.of(
                new OllamaChatRequest.ChatMessage("system", systemPrompt),
                new OllamaChatRequest.ChatMessage("user", userPrompt)
        ));

        try {
            OllamaChatResponse response = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            
            if (response != null && response.getMessage() != null) {
                return response.getMessage().getContent();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Ollama chat error: " + e.getMessage() + 
                    ". Ensure Ollama is running at configured base-url with model: " + defaultModel, e);
        }
    }
}