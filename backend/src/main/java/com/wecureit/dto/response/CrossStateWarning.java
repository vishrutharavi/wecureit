package com.wecureit.dto.response;

public class CrossStateWarning {

    private String referralId;
    private String fromDoctorName;
    private String toDoctorName;
    private String patientName;
    private String patientState;
    private String toDoctorState;

    public CrossStateWarning() {}

    public CrossStateWarning(String referralId, String fromDoctorName, String toDoctorName,
                              String patientName, String patientState, String toDoctorState) {
        this.referralId = referralId;
        this.fromDoctorName = fromDoctorName;
        this.toDoctorName = toDoctorName;
        this.patientName = patientName;
        this.patientState = patientState;
        this.toDoctorState = toDoctorState;
    }

    public String getReferralId() { return referralId; }
    public void setReferralId(String referralId) { this.referralId = referralId; }

    public String getFromDoctorName() { return fromDoctorName; }
    public void setFromDoctorName(String fromDoctorName) { this.fromDoctorName = fromDoctorName; }

    public String getToDoctorName() { return toDoctorName; }
    public void setToDoctorName(String toDoctorName) { this.toDoctorName = toDoctorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientState() { return patientState; }
    public void setPatientState(String patientState) { this.patientState = patientState; }

    public String getToDoctorState() { return toDoctorState; }
    public void setToDoctorState(String toDoctorState) { this.toDoctorState = toDoctorState; }
}
