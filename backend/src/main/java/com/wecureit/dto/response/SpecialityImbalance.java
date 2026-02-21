package com.wecureit.dto.response;

public class SpecialityImbalance {

    private String specialityCode;
    private String specialityName;
    private int totalReferrals;
    private int completedReferrals;
    private double completionRate;

    public SpecialityImbalance() {}

    public SpecialityImbalance(String specialityCode, String specialityName,
                                int totalReferrals, int completedReferrals, double completionRate) {
        this.specialityCode = specialityCode;
        this.specialityName = specialityName;
        this.totalReferrals = totalReferrals;
        this.completedReferrals = completedReferrals;
        this.completionRate = completionRate;
    }

    public String getSpecialityCode() { return specialityCode; }
    public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

    public String getSpecialityName() { return specialityName; }
    public void setSpecialityName(String specialityName) { this.specialityName = specialityName; }

    public int getTotalReferrals() { return totalReferrals; }
    public void setTotalReferrals(int totalReferrals) { this.totalReferrals = totalReferrals; }

    public int getCompletedReferrals() { return completedReferrals; }
    public void setCompletedReferrals(int completedReferrals) { this.completedReferrals = completedReferrals; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
}
