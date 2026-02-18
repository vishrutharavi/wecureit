package com.wecureit.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class CarePathResponse {

    private List<CarePathNode> nodes;
    private List<CarePathEdge> edges;

    public CarePathResponse() {}

    public CarePathResponse(List<CarePathNode> nodes, List<CarePathEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<CarePathNode> getNodes() { return nodes; }
    public void setNodes(List<CarePathNode> nodes) { this.nodes = nodes; }

    public List<CarePathEdge> getEdges() { return edges; }
    public void setEdges(List<CarePathEdge> edges) { this.edges = edges; }

    public static class CarePathNode {
        private String doctorId;
        private String doctorName;

        public CarePathNode() {}

        public CarePathNode(String doctorId, String doctorName) {
            this.doctorId = doctorId;
            this.doctorName = doctorName;
        }

        public String getDoctorId() { return doctorId; }
        public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

        public String getDoctorName() { return doctorName; }
        public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    }

    public static class CarePathEdge {
        private String referralId;
        private String specialityCode;
        private String status;
        private LocalDateTime createdAt;

        public CarePathEdge() {}

        public CarePathEdge(String referralId, String specialityCode, String status, LocalDateTime createdAt) {
            this.referralId = referralId;
            this.specialityCode = specialityCode;
            this.status = status;
            this.createdAt = createdAt;
        }

        public String getReferralId() { return referralId; }
        public void setReferralId(String referralId) { this.referralId = referralId; }

        public String getSpecialityCode() { return specialityCode; }
        public void setSpecialityCode(String specialityCode) { this.specialityCode = specialityCode; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
