package com.wecureit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotSuggestion {
    private String doctorId;
    private String doctorName;
    private String specialty;
    private String facilityId;
    private String facilityName;
    private String date;
    private String startTime;
    private String endTime;
    private Integer durationMinutes;
    private String reason;
}
