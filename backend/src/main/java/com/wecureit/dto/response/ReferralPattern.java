package com.wecureit.dto.response;

public class ReferralPattern {

    private String fromDoctorName;
    private String toDoctorName;
    private String speciality;
    private int frequency;

    public ReferralPattern() {}

    public ReferralPattern(String fromDoctorName, String toDoctorName, String speciality, int frequency) {
        this.fromDoctorName = fromDoctorName;
        this.toDoctorName = toDoctorName;
        this.speciality = speciality;
        this.frequency = frequency;
    }

    public String getFromDoctorName() { return fromDoctorName; }
    public void setFromDoctorName(String fromDoctorName) { this.fromDoctorName = fromDoctorName; }

    public String getToDoctorName() { return toDoctorName; }
    public void setToDoctorName(String toDoctorName) { this.toDoctorName = toDoctorName; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }
}
