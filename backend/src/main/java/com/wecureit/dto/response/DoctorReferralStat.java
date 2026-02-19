package com.wecureit.dto.response;

public class DoctorReferralStat {

    private String doctorId;
    private String doctorName;
    private int outgoingCount;
    private int incomingCount;

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public int getOutgoingCount() { return outgoingCount; }
    public void setOutgoingCount(int outgoingCount) { this.outgoingCount = outgoingCount; }

    public int getIncomingCount() { return incomingCount; }
    public void setIncomingCount(int incomingCount) { this.incomingCount = incomingCount; }
}
