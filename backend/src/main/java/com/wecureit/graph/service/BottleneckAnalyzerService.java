package com.wecureit.graph.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import com.wecureit.dto.response.BottleneckReport;
import com.wecureit.dto.response.CrossStateWarning;
import com.wecureit.dto.response.DoctorBottleneck;
import com.wecureit.dto.response.SpecialityImbalance;

@Service
@ConditionalOnProperty(name = "neo4j.enabled", havingValue = "true")
public class BottleneckAnalyzerService {

    private final Neo4jClient neo4jClient;

    public BottleneckAnalyzerService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public BottleneckReport generateReport() {
        BottleneckReport report = new BottleneckReport();
        report.setOverloadedSpecialists(findOverloadedSpecialists(5));
        report.setSpecialityImbalances(findSpecialityImbalances());
        report.setCrossStateWarnings(findCrossStateReferrals());
        report.setGeneratedAt(LocalDateTime.now());
        return report;
    }

    public List<DoctorBottleneck> findOverloadedSpecialists(int pendingThreshold) {
        Collection<DoctorBottleneck> results = neo4jClient.query(
                "MATCH (d:Doctor)<-[r:REFERRED_TO]-(from:Doctor) " +
                "WITH d, " +
                "  sum(CASE WHEN r.status = 'PENDING' THEN 1 ELSE 0 END) AS pendingCount, " +
                "  sum(CASE WHEN r.status = 'ACCEPTED' THEN 1 ELSE 0 END) AS acceptedCount " +
                "WHERE pendingCount >= $threshold " +
                "RETURN d.pgId AS doctorId, d.name AS doctorName, " +
                "  pendingCount, acceptedCount " +
                "ORDER BY pendingCount DESC")
            .bind(pendingThreshold).to("threshold")
            .fetchAs(DoctorBottleneck.class)
            .mappedBy((typeSystem, record) -> new DoctorBottleneck(
                    record.get("doctorId").asString(),
                    record.get("doctorName").asString(),
                    record.get("pendingCount").asInt(),
                    record.get("acceptedCount").asInt()))
            .all();

        return new ArrayList<>(results);
    }

    public List<SpecialityImbalance> findSpecialityImbalances() {
        Collection<SpecialityImbalance> results = neo4jClient.query(
                "MATCH ()-[r:REFERRED_TO]->() " +
                "WITH r.specialityCode AS specCode, " +
                "  count(r) AS total, " +
                "  sum(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed " +
                "WHERE total > 3 " +
                "OPTIONAL MATCH (s:Speciality {code: specCode}) " +
                "RETURN specCode, COALESCE(s.name, specCode) AS specName, " +
                "  total, completed, " +
                "  toFloat(completed) / total AS completionRate " +
                "ORDER BY completionRate ASC")
            .fetchAs(SpecialityImbalance.class)
            .mappedBy((typeSystem, record) -> new SpecialityImbalance(
                    record.get("specCode").asString(),
                    record.get("specName").asString(),
                    record.get("total").asInt(),
                    record.get("completed").asInt(),
                    record.get("completionRate").asDouble()))
            .all();

        return new ArrayList<>(results);
    }

    public List<CrossStateWarning> findCrossStateReferrals() {
        Collection<CrossStateWarning> results = neo4jClient.query(
                "MATCH (from:Doctor)-[r:REFERRED_TO]->(to:Doctor) " +
                "MATCH (p:Patient {pgId: r.patientPgId}) " +
                "WHERE p.stateCode IS NOT NULL " +
                "  AND p.stateCode <> '' " +
                "  AND NOT EXISTS { " +
                "    MATCH (to)-[:HAS_SPECIALITY {stateCode: p.stateCode}]->() " +
                "  } " +
                "RETURN r.referralId AS referralId, " +
                "  from.name AS fromDoctorName, " +
                "  to.name AS toDoctorName, " +
                "  p.name AS patientName, " +
                "  p.stateCode AS patientState, " +
                "  'Out-of-state' AS toDoctorState " +
                "ORDER BY r.createdAt DESC " +
                "LIMIT 50")
            .fetchAs(CrossStateWarning.class)
            .mappedBy((typeSystem, record) -> new CrossStateWarning(
                    record.get("referralId").asString(),
                    record.get("fromDoctorName").asString(),
                    record.get("toDoctorName").asString(),
                    record.get("patientName").asString(),
                    record.get("patientState").asString(),
                    record.get("toDoctorState").asString()))
            .all();

        return new ArrayList<>(results);
    }
}
