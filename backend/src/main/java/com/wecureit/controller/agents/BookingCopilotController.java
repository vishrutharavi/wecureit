package com.wecureit.controller.agents;

import com.wecureit.dto.request.BookingCopilotRequest;
import com.wecureit.dto.request.BookingIntent;
import com.wecureit.dto.response.BookingSuggestionsResponse;
import com.wecureit.dto.response.SlotSuggestion;
import com.wecureit.service.BookingCopilotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent/booking")
public class BookingCopilotController {
    
    private final BookingCopilotService bookingCopilotService;
    
    public BookingCopilotController(BookingCopilotService bookingCopilotService) {
        this.bookingCopilotService = bookingCopilotService;
    }
    
    @PostMapping("/interpret")
    public ResponseEntity<BookingIntent> interpretBookingIntent(
            @RequestBody BookingCopilotRequest request) {
        try {
            BookingIntent intent = bookingCopilotService.interpretUtterance(request.getUtterance());
            return ResponseEntity.ok(intent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/suggest")
    public ResponseEntity<BookingSuggestionsResponse> suggestSlots(
            @RequestBody BookingIntent intent) {
        try {
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
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/copilot")
    public ResponseEntity<BookingSuggestionsResponse> bookingCopilot(
            @RequestBody BookingCopilotRequest request) {
        try {
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
            return ResponseEntity.internalServerError().build();
        }
    }
}
