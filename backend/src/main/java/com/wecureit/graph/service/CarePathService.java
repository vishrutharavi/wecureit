package com.wecureit.graph.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import com.wecureit.dto.response.CarePathResponse;
import com.wecureit.dto.response.CarePathResponse.CarePathEdge;
import com.wecureit.dto.response.CarePathResponse.CarePathNode;
import com.wecureit.dto.response.ReferralPartner;
import com.wecureit.dto.response.ReferralPattern;

@Service
@ConditionalOnProperty(name = "neo4j.enabled", havingValue = "true")
public class CarePathService {

    private final Neo4jClient neo4jClient;

    public CarePathService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public CarePathResponse getPatientCarePath(UUID patientId) {
        String patientPgId = patientId.toString();

        // Get all referrals for this patient as ordered chain
        Collection<CarePathEdge> edges = neo4jClient.query(
                "MATCH (from:Doctor)-[r:REFERRED_TO {patientPgId: $patientId}]->(to:Doctor) " +
                "RETURN r.referralId AS referralId, " +
                "  r.specialityCode AS specialityCode, " +
                "  r.status AS status, " +
                "  r.createdAt AS createdAt, " +
                "  from.pgId AS fromId, from.name AS fromName, " +
                "  to.pgId AS toId, to.name AS toName " +
                "ORDER BY r.createdAt ASC")
            .bind(patientPgId).to("patientId")
            .fetchAs(CarePathEdge.class)
            .mappedBy((typeSystem, record) -> new CarePathEdge(
                    record.get("referralId").asString(),
                    record.get("specialityCode").asString(),
                    record.get("status").asString(),
                    record.get("createdAt").isNull() ? null : record.get("createdAt").asLocalDateTime()))
            .all();

        // Collect unique doctor nodes from the edges
        Collection<CarePathNode> nodes = neo4jClient.query(
                "MATCH (d:Doctor)-[r:REFERRED_TO {patientPgId: $patientId}]->() " +
                "WITH COLLECT(DISTINCT {pgId: d.pgId, name: d.name}) AS fromDocs " +
                "MATCH ()-[r2:REFERRED_TO {patientPgId: $patientId}]->(d2:Doctor) " +
                "WITH fromDocs + COLLECT(DISTINCT {pgId: d2.pgId, name: d2.name}) AS allDocs " +
                "UNWIND allDocs AS doc " +
                "WITH DISTINCT doc " +
                "RETURN doc.pgId AS doctorId, doc.name AS doctorName")
            .bind(patientPgId).to("patientId")
            .fetchAs(CarePathNode.class)
            .mappedBy((typeSystem, record) -> new CarePathNode(
                    record.get("doctorId").asString(),
                    record.get("doctorName").asString()))
            .all();

        return new CarePathResponse(new ArrayList<>(nodes), new ArrayList<>(edges));
    }

    public List<ReferralPartner> getTopReferralPartners(UUID doctorId) {
        String pgId = doctorId.toString();

        Collection<ReferralPartner> results = neo4jClient.query(
                "MATCH (from:Doctor {pgId: $doctorId})-[r:REFERRED_TO]->(to:Doctor) " +
                "WITH to, count(r) AS referralCount, " +
                "  collect(DISTINCT r.specialityCode) AS specialities " +
                "RETURN to.pgId AS doctorId, to.name AS doctorName, " +
                "  referralCount, specialities " +
                "ORDER BY referralCount DESC " +
                "LIMIT 10")
            .bind(pgId).to("doctorId")
            .fetchAs(ReferralPartner.class)
            .mappedBy((typeSystem, record) -> new ReferralPartner(
                    record.get("doctorId").asString(),
                    record.get("doctorName").asString(),
                    record.get("referralCount").asInt(),
                    record.get("specialities").asList(org.neo4j.driver.Value::asString)))
            .all();

        return new ArrayList<>(results);
    }

    public List<ReferralPattern> getCommonPatterns() {
        Collection<ReferralPattern> results = neo4jClient.query(
                "MATCH (from:Doctor)-[r:REFERRED_TO]->(to:Doctor) " +
                "WITH from.name AS fromName, to.name AS toName, count(r) AS frequency, " +
                "  collect(DISTINCT r.specialityCode) AS specialities " +
                "WHERE frequency > 1 " +
                "UNWIND specialities AS speciality " +
                "RETURN fromName, toName, speciality, frequency " +
                "ORDER BY frequency DESC " +
                "LIMIT 20")
            .fetchAs(ReferralPattern.class)
            .mappedBy((typeSystem, record) -> new ReferralPattern(
                    record.get("fromName").asString(),
                    record.get("toName").asString(),
                    record.get("speciality").asString(),
                    record.get("frequency").asInt()))
            .all();

        return new ArrayList<>(results);
    }

    public CarePathResponse getShortestPath(UUID fromDoctorId, UUID toDoctorId) {
        String fromId = fromDoctorId.toString();
        String toId = toDoctorId.toString();

        // Get shortest path nodes
        Collection<CarePathNode> nodes = neo4jClient.query(
                "MATCH path = shortestPath(" +
                "  (from:Doctor {pgId: $fromId})-[:REFERRED_TO*]-(to:Doctor {pgId: $toId})" +
                ") " +
                "UNWIND nodes(path) AS n " +
                "RETURN n.pgId AS doctorId, n.name AS doctorName")
            .bind(fromId).to("fromId")
            .bind(toId).to("toId")
            .fetchAs(CarePathNode.class)
            .mappedBy((typeSystem, record) -> new CarePathNode(
                    record.get("doctorId").asString(),
                    record.get("doctorName").asString()))
            .all();

        // Get shortest path relationships
        Collection<CarePathEdge> edges = neo4jClient.query(
                "MATCH path = shortestPath(" +
                "  (from:Doctor {pgId: $fromId})-[:REFERRED_TO*]-(to:Doctor {pgId: $toId})" +
                ") " +
                "UNWIND relationships(path) AS r " +
                "RETURN r.referralId AS referralId, r.specialityCode AS specialityCode, " +
                "  r.status AS status, r.createdAt AS createdAt")
            .bind(fromId).to("fromId")
            .bind(toId).to("toId")
            .fetchAs(CarePathEdge.class)
            .mappedBy((typeSystem, record) -> new CarePathEdge(
                    record.get("referralId").asString(),
                    record.get("specialityCode").asString(),
                    record.get("status").asString(),
                    record.get("createdAt").isNull() ? null : record.get("createdAt").asLocalDateTime()))
            .all();

        return new CarePathResponse(new ArrayList<>(nodes), new ArrayList<>(edges));
    }
}
