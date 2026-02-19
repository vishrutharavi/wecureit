package com.wecureit.event;

import java.util.UUID;

public class ReferralStatusChangedEvent {

    private final UUID referralId;
    private final String oldStatus;
    private final String newStatus;

    public ReferralStatusChangedEvent(UUID referralId, String oldStatus, String newStatus) {
        this.referralId = referralId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public UUID getReferralId() { return referralId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
}
