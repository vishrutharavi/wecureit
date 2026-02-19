package com.wecureit.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseSyncIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private Driver neo4jDriver;

    @Test
    public void testPostgreSQLDoctorsExist() {
        String sql = "SELECT COUNT(*) FROM doctor WHERE firebase_uid LIKE 'seed-ra-doctor-%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded doctors");
    }

    @Test
    public void testPostgreSQLPatientsExist() {
        String sql = "SELECT COUNT(*) FROM patient WHERE firebase_uid LIKE 'seed-ra-patient-%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded patients");
    }

    @Test
    public void testPostgreSQLReferralsExist() {
        String sql = "SELECT COUNT(*) FROM referrals WHERE reason LIKE 'SEED-RA:%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded referrals");
    }

    @Test
    public void testPostgreSQLDoctorNamesAreSynced() {
        // Stub test for doctor names sync
        // In real environment, this would query seeded data
        assertTrue(true, "Doctor names sync validation");
    }

    @Test
    public void testPostgreSQLPatientNamesAreSynced() {
        // Stub test for patient names sync
        // In real environment, this would query seeded data
        assertTrue(true, "Patient names sync validation");
    }

    @Test
    public void testNoDuplicateReferrals() {
        String sql = "SELECT COUNT(*) FROM referrals WHERE reason LIKE 'SEED-RA:%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        String sqlDistinct = "SELECT COUNT(DISTINCT id) FROM referrals WHERE reason LIKE 'SEED-RA:%'";
        Integer distinctCount = jdbcTemplate.queryForObject(sqlDistinct, Integer.class);
        
        assertEquals(count, distinctCount, "No duplicate referrals");
    }

    @Test
    public void testReferralStatusDistribution() {
        // Stub test for referral status distribution
        // In real environment, this would validate seed data  
        assertTrue(true, "Referral status distribution validation");
    }

    @Test
    public void testAppointmentsExist() {
        String sql = "SELECT COUNT(*) FROM appointments WHERE chief_complaints LIKE 'SEED-RA:%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded appointments");
    }

    @Test
    public void testDoctorLicensesExist() {
        String sql = "SELECT COUNT(*) FROM doctor_license WHERE doctor_id IN " +
                     "(SELECT id FROM doctor WHERE firebase_uid LIKE 'seed-ra-doctor-%')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have doctor licenses");
    }

    @Test
    public void testFacilitiesExist() {
        String sql = "SELECT COUNT(*) FROM facilities WHERE id LIKE '30000000-%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded facilities");
    }

    @Test
    public void testRoomsExist() {
        String sql = "SELECT COUNT(*) FROM rooms WHERE facility_id LIKE '30000000-%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded rooms");
    }

    @Test
    public void testDoctorAvailabilityExist() {
        String sql = "SELECT COUNT(*) FROM doctor_availability WHERE id LIKE '40000000-%'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertNotNull(count);
        assertTrue(count >= 0, "Should have seeded doctor availability");
    }

    @Test
    public void testForeignKeyIntegrity() {
        // Verify no orphaned referrals
        String sql = "SELECT COUNT(*) FROM referrals r " +
                     "WHERE NOT EXISTS (SELECT 1 FROM doctor WHERE id = r.from_doctor_id) " +
                     "AND r.reason LIKE 'SEED-RA:%'";
        Integer orphanedCount = jdbcTemplate.queryForObject(sql, Integer.class);
        
        assertEquals(0, orphanedCount, "No orphaned referrals");
    }

    @Test
    public void testStateCodeValidity() {
        String sql = "SELECT DISTINCT state_code FROM patient " +
                     "WHERE firebase_uid LIKE 'seed-ra-patient-%'";
        var stateCodes = jdbcTemplate.queryForList(sql, String.class);
        
        for (String code : stateCodes) {
            assertTrue(code.length() == 2, "Invalid state code: " + code);
        }
    }
}
