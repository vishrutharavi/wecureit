package com.wecureit.graph.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.wecureit.entity.Doctor;
import com.wecureit.entity.Patient;
import com.wecureit.entity.Referral;
import com.wecureit.entity.Speciality;
import com.wecureit.event.ReferralCreatedEvent;
import com.wecureit.event.ReferralStatusChangedEvent;
import com.wecureit.graph.node.DoctorNode;
import com.wecureit.graph.node.PatientNode;
import com.wecureit.graph.node.SpecialityNode;
import com.wecureit.graph.repository.DoctorNodeRepository;
import com.wecureit.graph.repository.PatientNodeRepository;
import com.wecureit.graph.repository.SpecialityNodeRepository;
import com.wecureit.repository.DoctorRepository;
import com.wecureit.repository.PatientRepository;
import com.wecureit.repository.ReferralRepository;
import com.wecureit.repository.SpecialityRepository;

@Service
@ConditionalOnProperty(name = "neo4j.enabled", havingValue = "true")
public class GraphSyncService {

    private static final Logger log = LoggerFactory.getLogger(GraphSyncService.class);

    private final DoctorNodeRepository doctorNodeRepository;
    private final PatientNodeRepository patientNodeRepository;
    private final SpecialityNodeRepository specialityNodeRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SpecialityRepository specialityRepository;
    private final ReferralRepository referralRepository;
    private final Neo4jClient neo4jClient;

    public GraphSyncService(
            DoctorNodeRepository doctorNodeRepository,
            PatientNodeRepository patientNodeRepository,
            SpecialityNodeRepository specialityNodeRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            SpecialityRepository specialityRepository,
            ReferralRepository referralRepository,
            Neo4jClient neo4jClient) {
        this.doctorNodeRepository = doctorNodeRepository;
        this.patientNodeRepository = patientNodeRepository;
        this.specialityNodeRepository = specialityNodeRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.specialityRepository = specialityRepository;
        this.referralRepository = referralRepository;
        this.neo4jClient = neo4jClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReferralCreated(ReferralCreatedEvent event) {
        try {
            log.info("Graph sync: processing referral created {}", event.getReferralId());

            ensureDoctorNode(event.getFromDoctorId());
            ensureDoctorNode(event.getToDoctorId());
            ensurePatientNode(event.getPatientId());
            ensureSpecialityNode(event.getSpecialityCode());
            createReferralRelationship(event);

            log.info("Graph sync: completed for referral {}", event.getReferralId());
        } catch (Exception e) {
            log.error("Graph sync failed for referral {}: {}", event.getReferralId(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReferralStatusChanged(ReferralStatusChangedEvent event) {
        try {
            log.info("Graph sync: updating referral {} status to {}", event.getReferralId(), event.getNewStatus());

            neo4jClient.query(
                    "MATCH (:Doctor)-[r:REFERRED_TO {referralId: $referralId}]->(:Doctor) " +
                    "SET r.status = $newStatus, r.updatedAt = localdatetime()")
                .bind(event.getReferralId().toString()).to("referralId")
                .bind(event.getNewStatus()).to("newStatus")
                .run();

            log.info("Graph sync: status updated for referral {}", event.getReferralId());
        } catch (Exception e) {
            log.error("Graph sync status update failed for referral {}: {}", event.getReferralId(), e.getMessage(), e);
        }
    }

    public void syncAllReferrals() {
        log.info("Graph sync: starting full bulk sync");
        List<Referral> allReferrals = referralRepository.findAll();
        int synced = 0;

        for (Referral referral : allReferrals) {
            try {
                ensureDoctorNode(referral.getFromDoctor().getId());
                ensureDoctorNode(referral.getToDoctor().getId());
                ensurePatientNode(referral.getPatient().getId());
                ensureSpecialityNode(referral.getSpeciality().getSpecialityCode());

                ReferralCreatedEvent event = new ReferralCreatedEvent(
                        referral.getId(),
                        referral.getFromDoctor().getId(),
                        referral.getToDoctor().getId(),
                        referral.getPatient().getId(),
                        referral.getSpeciality().getSpecialityCode(),
                        referral.getReason());
                createReferralRelationship(event);

                // Update status if not PENDING
                if (!"PENDING".equals(referral.getStatus())) {
                    neo4jClient.query(
                            "MATCH (:Doctor)-[r:REFERRED_TO {referralId: $referralId}]->(:Doctor) " +
                            "SET r.status = $status, r.updatedAt = localdatetime()")
                        .bind(referral.getId().toString()).to("referralId")
                        .bind(referral.getStatus()).to("status")
                        .run();
                }

                synced++;
            } catch (Exception e) {
                log.error("Graph sync: failed to sync referral {}: {}", referral.getId(), e.getMessage(), e);
            }
        }

        log.info("Graph sync: bulk sync completed. Synced {}/{} referrals", synced, allReferrals.size());
    }

    private DoctorNode ensureDoctorNode(UUID doctorId) {
        String pgId = doctorId.toString();
        return doctorNodeRepository.findById(pgId)
                .orElseGet(() -> {
                    Doctor doc = doctorRepository.findById(doctorId)
                            .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));
                    DoctorNode node = new DoctorNode(pgId, doc.getName(), doc.getEmail(), doc.getIsActive());
                    return doctorNodeRepository.save(node);
                });
    }

    private PatientNode ensurePatientNode(UUID patientId) {
        String pgId = patientId.toString();
        return patientNodeRepository.findById(pgId)
                .orElseGet(() -> {
                    Patient patient = patientRepository.findById(patientId)
                            .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
                    String stateCode = patient.getState() != null ? patient.getState().getStateCode() : null;
                    PatientNode node = new PatientNode(pgId, patient.getName(), stateCode);
                    return patientNodeRepository.save(node);
                });
    }

    private SpecialityNode ensureSpecialityNode(String specialityCode) {
        return specialityNodeRepository.findById(specialityCode)
                .orElseGet(() -> {
                    Speciality spec = specialityRepository.findById(specialityCode)
                            .orElseThrow(() -> new RuntimeException("Speciality not found: " + specialityCode));
                    SpecialityNode node = new SpecialityNode(spec.getSpecialityCode(), spec.getSpecialityName());
                    return specialityNodeRepository.save(node);
                });
    }

    private void createReferralRelationship(ReferralCreatedEvent event) {
        neo4jClient.query(
                "MATCH (from:Doctor {pgId: $fromId}), (to:Doctor {pgId: $toId}) " +
                "CREATE (from)-[:REFERRED_TO {" +
                "  referralId: $referralId, " +
                "  patientPgId: $patientId, " +
                "  specialityCode: $specialityCode, " +
                "  status: 'PENDING', " +
                "  reason: $reason, " +
                "  createdAt: localdatetime(), " +
                "  updatedAt: localdatetime()" +
                "}]->(to)")
            .bind(event.getFromDoctorId().toString()).to("fromId")
            .bind(event.getToDoctorId().toString()).to("toId")
            .bind(event.getReferralId().toString()).to("referralId")
            .bind(event.getPatientId().toString()).to("patientId")
            .bind(event.getSpecialityCode()).to("specialityCode")
            .bind(event.getReason()).to("reason")
            .run();
    }
}
