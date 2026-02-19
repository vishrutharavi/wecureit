package com.wecureit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimalSlotRequest {
    private UUID doctorId;
    private UUID facilityId;
    private String specialtyCode;
    private Integer duration;           // 15, 30, or 60 (defaults to 30)
    private String dateRangeStart;      // YYYY-MM-DD (optional, defaults to today)
    private String dateRangeEnd;        // YYYY-MM-DD (optional, defaults to today + 14 days)
}
