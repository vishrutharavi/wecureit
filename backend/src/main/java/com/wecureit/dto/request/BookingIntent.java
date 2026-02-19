package com.wecureit.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingIntent {
    private String specialty;
    private String facilityName;
    private String doctorName;
    private String preferredDateStart;
    private String preferredDateEnd;
    private String preferredTimeStart;
    private String preferredTimeEnd;
    private Integer durationMinutes;
}
