package com.wecureit.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CardRequest {
    private String pan;
    private String cvc;
    private int expMonth;
    private int expYear;
    // patient id (UUID) matching Patient.id
    private UUID patientMasterId;
}
