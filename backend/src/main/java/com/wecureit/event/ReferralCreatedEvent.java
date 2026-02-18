package com.wecureit.event;

import java.util.UUID;

public class ReferralCreatedEvent {

    private final UUID referralId;
    private final UUID fromDoctorId;
    private final UUID toDoctorId;
    private final UUID patientId;
    private final String specialityCode;
    private final String reason;

    public ReferralCreatedEvent(UUID referralId, UUID fromDoctorId, UUID toDoctorId,
                                 UUID patientId, String specialityCode, String reason) {
        this.referralId = referralId;
        this.fromDoctorId = fromDoctorId;
        this.toDoctorId = toDoctorId;
        this.patientId = patientId;
        this.specialityCode = specialityCode;
        this.reason = reason;
    }

    public UUID getReferralId() { return referralId; }
    public UUID getFromDoctorId() { return fromDoctorId; }
    public UUID getToDoctorId() { return toDoctorId; }
    public UUID getPatientId() { return patientId; }
    public String getSpecialityCode() { return specialityCode; }
    public String getReason() { return reason; }
}
