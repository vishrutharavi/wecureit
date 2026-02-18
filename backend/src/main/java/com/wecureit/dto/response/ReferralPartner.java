package com.wecureit.dto.response;

import java.util.List;

public class ReferralPartner {

    private String doctorId;
    private String doctorName;
    private int referralCount;
    private List<String> specialities;

    public ReferralPartner() {}

    public ReferralPartner(String doctorId, String doctorName, int referralCount, List<String> specialities) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.referralCount = referralCount;
        this.specialities = specialities;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public int getReferralCount() { return referralCount; }
    public void setReferralCount(int referralCount) { this.referralCount = referralCount; }

    public List<String> getSpecialities() { return specialities; }
    public void setSpecialities(List<String> specialities) { this.specialities = specialities; }
}
