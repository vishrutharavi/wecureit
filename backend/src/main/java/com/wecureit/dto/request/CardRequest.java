package com.wecureit.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter @Setter
public class CardRequest {
    private String pan;
    private String cvc;
    private int expMonth;
    private int expYear;
    // patient id (UUID) matching Patient.id
    private UUID patientMasterId;
}
