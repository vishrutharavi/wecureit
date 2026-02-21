package com.wecureit.controller.agents;

import com.wecureit.dto.request.BookingCopilotRequest;
import com.wecureit.dto.request.BookingIntent;
import com.wecureit.dto.response.BookingSuggestionsResponse;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.service.BookingCopilotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/booking")
public class BookingCopilotController {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingCopilotController.class);
    private final BookingCopilotService bookingCopilotService;
    
    public BookingCopilotController(BookingCopilotService bookingCopilotService) {
        this.bookingCopilotService = bookingCopilotService;
    }
    
    @PostMapping("/interpret")
    public ResponseEntity<?> interpretBookingIntent(
            @RequestBody BookingCopilotRequest request) {
        try {
            logger.info("Interpreting booking utterance: {}", request.getUtterance());
            BookingIntent intent = bookingCopilotService.interpretUtterance(request.getUtterance());
            return ResponseEntity.ok(intent);
        } catch (Exception e) {
            logger.error("Error interpreting booking intent", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/suggest")
    public ResponseEntity<?> suggestSlots(
            @RequestBody BookingIntent intent) {
        try {
            logger.info("Suggesting slots for intent: {}", intent);
            List<SlotSuggestion> suggestions = bookingCopilotService.suggestSlots(intent);
            List<SlotSuggestion> alternatives = suggestions.isEmpty() 
                    ? bookingCopilotService.suggestAlternatives(intent) 
                    : List.of();
            
            String message = suggestions.isEmpty() 
                    ? "No exact matches found. Here are some alternative options:"
                    : "Top suggestions based on your preferences:";
            
            BookingSuggestionsResponse response = new BookingSuggestionsResponse(
                    suggestions, alternatives, message);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error suggesting slots", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/copilot")
    public ResponseEntity<?> bookingCopilot(
            @RequestBody BookingCopilotRequest request) {
        try {
            logger.info("Running booking copilot for utterance: {}", request.getUtterance());
            BookingIntent intent = bookingCopilotService.interpretUtterance(request.getUtterance());
            
            List<SlotSuggestion> suggestions = bookingCopilotService.suggestSlots(intent);
            List<SlotSuggestion> alternatives = suggestions.isEmpty() 
                    ? bookingCopilotService.suggestAlternatives(intent) 
                    : List.of();
            
            String message = suggestions.isEmpty() 
                    ? "No exact matches found. Here are some alternative options:"
                    : "Top suggestions based on your preferences:";
            
            BookingSuggestionsResponse response = new BookingSuggestionsResponse(
                    suggestions, alternatives, message);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in booking copilot", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
