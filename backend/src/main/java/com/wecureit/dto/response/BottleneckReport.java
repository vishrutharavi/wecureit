package com.wecureit.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class BottleneckReport {

    private List<DoctorBottleneck> overloadedSpecialists;
    private List<SpecialityImbalance> specialityImbalances;
    private List<CrossStateWarning> crossStateWarnings;
    private LocalDateTime generatedAt;

    public List<DoctorBottleneck> getOverloadedSpecialists() { return overloadedSpecialists; }
    public void setOverloadedSpecialists(List<DoctorBottleneck> overloadedSpecialists) { this.overloadedSpecialists = overloadedSpecialists; }

    public List<SpecialityImbalance> getSpecialityImbalances() { return specialityImbalances; }
    public void setSpecialityImbalances(List<SpecialityImbalance> specialityImbalances) { this.specialityImbalances = specialityImbalances; }

    public List<CrossStateWarning> getCrossStateWarnings() { return crossStateWarnings; }
    public void setCrossStateWarnings(List<CrossStateWarning> crossStateWarnings) { this.crossStateWarnings = crossStateWarnings; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
