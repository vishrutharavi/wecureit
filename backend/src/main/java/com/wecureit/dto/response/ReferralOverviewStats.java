package com.wecureit.dto.response;

public class ReferralOverviewStats {

    private int totalReferrals;
    private int pendingCount;
    private int acceptedCount;
    private int completedCount;
    private int cancelledCount;
    private double completionRate;
    private double avgResponseTimeHours;
    private String topSpeciality;

    public int getTotalReferrals() { return totalReferrals; }
    public void setTotalReferrals(int totalReferrals) { this.totalReferrals = totalReferrals; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(int acceptedCount) { this.acceptedCount = acceptedCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public int getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(int cancelledCount) { this.cancelledCount = cancelledCount; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public double getAvgResponseTimeHours() { return avgResponseTimeHours; }
    public void setAvgResponseTimeHours(double avgResponseTimeHours) { this.avgResponseTimeHours = avgResponseTimeHours; }

    public String getTopSpeciality() { return topSpeciality; }
    public void setTopSpeciality(String topSpeciality) { this.topSpeciality = topSpeciality; }
}
