package com.wecureit.dto.response;

public class DoctorBottleneck {

    private String doctorId;
    private String doctorName;
    private int pendingReferrals;
    private int acceptedReferrals;
    private String severity;

    public DoctorBottleneck() {}

    public DoctorBottleneck(String doctorId, String doctorName, int pendingReferrals, int acceptedReferrals) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.pendingReferrals = pendingReferrals;
        this.acceptedReferrals = acceptedReferrals;
        this.severity = pendingReferrals > 20 ? "HIGH" : pendingReferrals > 10 ? "MEDIUM" : "LOW";
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public int getPendingReferrals() { return pendingReferrals; }
    public void setPendingReferrals(int pendingReferrals) { this.pendingReferrals = pendingReferrals; }

    public int getAcceptedReferrals() { return acceptedReferrals; }
    public void setAcceptedReferrals(int acceptedReferrals) { this.acceptedReferrals = acceptedReferrals; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
