package com.wecureit.graph.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    @EventListener(ApplicationReadyEvent.class)
    public void verifyAndSyncOnStartup() {
        try {
            log.info("Verifying graph sync on application startup...");
            syncMissingReferrals();
        } catch (Exception e) {
            log.error("Failed to verify graph sync on startup: {}", e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReferralCreated(ReferralCreatedEvent event) {
        try {
            log.info("Graph sync: processing referral created {}", event.getReferralId());

            ensureDoctorNode(event.getFromDoctorId());
            // Only ensure destination doctor if different
            if (!event.getFromDoctorId().equals(event.getToDoctorId())) {
                ensureDoctorNode(event.getToDoctorId());
            }
            ensurePatientNode(event.getPatientId());
            ensureSpecialityNode(event.getSpecialityCode());

            // Create referral relationship with error handling
            neo4jClient.query(
                    "MATCH (from:Doctor {pgId: $fromId}) " +
                    "MATCH (to:Doctor {pgId: $toId}) " +
                    "MERGE (from)-[r:REFERRED_TO {referralId: $referralId}]->(to) " +
                    "SET r.patientPgId = $patientId, " +
                    "    r.specialityCode = $specialityCode, " +
                    "    r.status = 'PENDING', " +
                    "    r.reason = $reason, " +
                    "    r.createdAt = localdatetime(), " +
                    "    r.updatedAt = localdatetime()")
                .bind(event.getFromDoctorId().toString()).to("fromId")
                .bind(event.getToDoctorId().toString()).to("toId")
                .bind(event.getReferralId().toString()).to("referralId")
                .bind(event.getPatientId().toString()).to("patientId")
                .bind(event.getSpecialityCode()).to("specialityCode")
                .bind(event.getReason()).to("reason")
                .run();

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
        int failed = 0;

        for (Referral referral : allReferrals) {
            try {
                UUID fromDoctorId = referral.getFromDoctor().getId();
                UUID toDoctorId = referral.getToDoctor().getId();
                
                // Skip if same doctor (self-referrals not supported in this context)
                // Actually, allow self-referrals but ensure both nodes exist
                ensureDoctorNode(fromDoctorId);
                if (!fromDoctorId.equals(toDoctorId)) {
                    ensureDoctorNode(toDoctorId);
                } // else: already ensured above
                
                ensurePatientNode(referral.getPatient().getId());
                ensureSpecialityNode(referral.getSpeciality().getSpecialityCode());

                // Create or update relationship (using MERGE to handle duplicates)
                neo4jClient.query(
                        "MATCH (from:Doctor {pgId: $fromId}) " +
                        "MATCH (to:Doctor {pgId: $toId}) " +
                        "MERGE (from)-[r:REFERRED_TO {referralId: $referralId}]->(to) " +
                        "SET r.patientPgId = $patientId, " +
                        "    r.specialityCode = $specialityCode, " +
                        "    r.status = $status, " +
                        "    r.reason = $reason, " +
                        "    r.createdAt = localdatetime(), " +
                        "    r.updatedAt = localdatetime()")
                    .bind(fromDoctorId.toString()).to("fromId")
                    .bind(toDoctorId.toString()).to("toId")
                    .bind(referral.getId().toString()).to("referralId")
                    .bind(referral.getPatient().getId().toString()).to("patientId")
                    .bind(referral.getSpeciality().getSpecialityCode()).to("specialityCode")
                    .bind(referral.getStatus()).to("status")
                    .bind(referral.getReason()).to("reason")
                    .run();

                synced++;
                log.debug("Synced referral {} ({}→{})", referral.getId(), 
                    referral.getFromDoctor().getName(), referral.getToDoctor().getName());
            } catch (Exception e) {
                failed++;
                log.error("Graph sync: failed to sync referral {}: {}", 
                    referral.getId(), e.getMessage(), e);
            }
        }

        log.info("Graph sync: bulk sync completed. Synced {}/{} referrals, {} failed", 
            synced, allReferrals.size(), failed);
    }

    public void syncMissingReferrals() {
        log.info("Graph sync: checking for missing referrals...");
        List<Referral> allReferrals = referralRepository.findAll();
        
        // Find which referrals are missing from Neo4j
        int missingSynced = 0;
        
        for (Referral referral : allReferrals) {
            try {
                String referralId = referral.getId().toString();
                
                // Check if this referral exists in Neo4j
                Collection<String> result = neo4jClient.query(
                        "MATCH ()-[r:REFERRED_TO {referralId: $referralId}]->() RETURN r.referralId")
                    .bind(referralId).to("referralId")
                    .fetchAs(String.class)
                    .all();
                
                // If not found, sync it
                if (result.isEmpty()) {
                    log.info("Missing referral {} found in PostgreSQL, syncing to Neo4j", referralId);
                    
                    UUID fromDoctorId = referral.getFromDoctor().getId();
                    UUID toDoctorId = referral.getToDoctor().getId();
                    
                    ensureDoctorNode(fromDoctorId);
                    if (!fromDoctorId.equals(toDoctorId)) {
                        ensureDoctorNode(toDoctorId);
                    }
                    ensurePatientNode(referral.getPatient().getId());
                    ensureSpecialityNode(referral.getSpeciality().getSpecialityCode());

                    neo4jClient.query(
                            "MATCH (from:Doctor {pgId: $fromId}) " +
                            "MATCH (to:Doctor {pgId: $toId}) " +
                            "MERGE (from)-[r:REFERRED_TO {referralId: $referralId}]->(to) " +
                            "SET r.patientPgId = $patientId, " +
                            "    r.specialityCode = $specialityCode, " +
                            "    r.status = $status, " +
                            "    r.reason = $reason")
                        .bind(fromDoctorId.toString()).to("fromId")
                        .bind(toDoctorId.toString()).to("toId")
                        .bind(referralId).to("referralId")
                        .bind(referral.getPatient().getId().toString()).to("patientId")
                        .bind(referral.getSpeciality().getSpecialityCode()).to("specialityCode")
                        .bind(referral.getStatus()).to("status")
                        .bind(referral.getReason()).to("reason")
                        .run();
                    
                    missingSynced++;
                    log.info("Synced missing referral {}", referralId);
                }
            } catch (Exception e) {
                log.error("Error checking/syncing referral {}: {}", referral.getId(), e.getMessage(), e);
            }
        }
        
        if (missingSynced > 0) {
            log.info("Graph sync: recovered {} missing referrals", missingSynced);
        } else {
            log.info("Graph sync: all referrals are in sync");
        }
    }

    private DoctorNode ensureDoctorNode(UUID doctorId) {
        String pgId = doctorId.toString();
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + doctorId));
        
        // Use Neo4j client directly to ensure doctor node exists
        neo4jClient.query(
                "MERGE (d:Doctor {pgId: $pgId}) " +
                "SET d.name = $name, d.email = $email, d.isActive = $isActive")
            .bind(pgId).to("pgId")
            .bind(doc.getName()).to("name")
            .bind(doc.getEmail()).to("email")
            .bind(doc.getIsActive()).to("isActive")
            .run();
        
        return doctorNodeRepository.findById(pgId).orElse(null);
    }

    private PatientNode ensurePatientNode(UUID patientId) {
        String pgId = patientId.toString();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        String stateCode = patient.getState() != null ? patient.getState().getStateCode() : null;
        
        // Use Neo4j client directly to ensure patient node exists
        neo4jClient.query(
                "MERGE (p:Patient {pgId: $pgId}) " +
                "SET p.name = $name, p.stateCode = $stateCode")
            .bind(pgId).to("pgId")
            .bind(patient.getName()).to("name")
            .bind(stateCode).to("stateCode")
            .run();
        
        return patientNodeRepository.findById(pgId).orElse(null);
    }

    private SpecialityNode ensureSpecialityNode(String specialityCode) {
        // Use Neo4j client directly to ensure speciality node exists
        Speciality spec = specialityRepository.findById(specialityCode)
                .orElseThrow(() -> new RuntimeException("Speciality not found: " + specialityCode));
        
        neo4jClient.query(
                "MERGE (s:Speciality {code: $code}) " +
                "SET s.name = $name")
            .bind(specialityCode).to("code")
            .bind(spec.getSpecialityName()).to("name")
            .run();
        
        return specialityNodeRepository.findById(specialityCode).orElse(null);
    }
}
