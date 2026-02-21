package com.wecureit.dto.response;

import lombok.Data;

@Data
public class OllamaChatResponse {
    private ChatMessage message;
    private boolean done;
    
    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
