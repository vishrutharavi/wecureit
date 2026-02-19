package com.wecureit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingSuggestionsResponse {
    private List<SlotSuggestion> suggestions;
    private List<SlotSuggestion> alternatives;
    private String message;
}
