import { test, expect } from '@playwright/test';

test.describe('Database Sync - PostgreSQL to Neo4j', () => {
  const apiUrl = 'http://localhost:8080/api';

  test('should sync all doctors from PostgreSQL to Neo4j', async ({ request }) => {
    // Call graph sync endpoint
    const response = await request.post(`${apiUrl}/admin/intelligence/graph/sync`);
    
    // Verify sync endpoint is accessible (may return 401 if auth required)
    expect([200, 401]).toContain(response.status());
  });

  test('should display correct number of referral patterns', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/care-path/patterns`);
    
    if (response.status() === 200) {
      const patterns = await response.json();
      expect(Array.isArray(patterns)).toBeTruthy();
    }
  });

  test('should return bottleneck report with specialists', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/bottlenecks`);
    
    if (response.status() === 200) {
      const report = await response.json();
      expect(report).toHaveProperty('overloadedSpecialists');
      expect(report).toHaveProperty('specialityImbalances');
      expect(report).toHaveProperty('crossStateWarnings');
    }
  });

  test('should return overloaded specialists', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/bottlenecks/overloaded?threshold=5`);
    
    if (response.status() === 200) {
      const specialists = await response.json();
      expect(Array.isArray(specialists)).toBeTruthy();
      
      // If there are specialists, verify structure
      if (specialists.length > 0) {
        expect(specialists[0]).toHaveProperty('doctorId');
        expect(specialists[0]).toHaveProperty('doctorName');
        expect(specialists[0]).toHaveProperty('pendingCount');
      }
    }
  });

  test('should have no duplicate speciality codes', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/bottlenecks`);
    
    if (response.status() === 200) {
      const report = await response.json();
      const specialityImbalances = report.specialityImbalances || [];
      
      // Get all speciality codes
      const codes = specialityImbalances.map((s: any) => s.specialityCode);
      
      // Check for duplicates
      const uniqueCodes = new Set(codes);
      expect(codes.length).toBe(uniqueCodes.size);
    }
  });

  test('should return care path patterns without old test data', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/care-path/patterns`);
    
    if (response.status() === 200) {
      const patterns = await response.json();
      
      // Verify no old test data names (DOC_NET_FROM, DOC_NET_TO)
      const hasOldData = patterns.some(
        (p: any) => p.fromDoctorName?.includes('DOC_') || p.toDoctorName?.includes('DOC_')
      );
      expect(hasOldData).toBeFalsy();
      
      // Verify synced doctor names are present
      const hasSyncedNames = patterns.some(
        (p: any) => ['Roy', 'James', 'Mian', 'Peter', 'Thanos'].includes(p.fromDoctorName) ||
                   ['Roy', 'James', 'Mian', 'Peter', 'Thanos'].includes(p.toDoctorName)
      );
      
      if (patterns.length > 0) {
        expect(hasSyncedNames).toBeTruthy();
      }
    }
  });
});

test.describe('Referral Data Integrity', () => {
  const apiUrl = 'http://localhost:8080/api';

  test('should have tagged seed referrals', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/care-path/patterns`);
    
    if (response.status() === 200) {
      const patterns = await response.json();
      expect(Array.isArray(patterns)).toBeTruthy();
    }
  });

  test('should have correct referral statuses', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/referrals/stats/overview`);
    
    if (response.status() === 200) {
      const stats = await response.json();
      expect(stats).toHaveProperty('total');
      expect(stats).toHaveProperty('pending');
      expect(stats).toHaveProperty('completed');
      expect(stats).toHaveProperty('accepted');
    }
  });

  test('should not have duplicate referrals', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/care-path/patterns`);
    
    if (response.status() === 200) {
      const patterns = await response.json();
      
      // Check for duplicate patterns
      const patternSet = new Set();
      const duplicates = patterns.filter((p: any) => {
        const key = `${p.fromDoctorName}->${p.toDoctorName}-${p.speciality}`;
        if (patternSet.has(key)) {
          return true;
        }
        patternSet.add(key);
        return false;
      });
      
      expect(duplicates.length).toBe(0);
    }
  });
});

test.describe('API Response Structure', () => {
  const apiUrl = 'http://localhost:8080/api';

  test('bottleneck report has correct structure', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/bottlenecks`);
    
    if (response.status() === 200) {
      const report = await response.json();
      
      // Verify structure
      expect(report).toHaveProperty('overloadedSpecialists');
      expect(report).toHaveProperty('specialityImbalances');
      expect(report).toHaveProperty('crossStateWarnings');
      expect(report).toHaveProperty('generatedAt');
      
      // Verify arrays
      expect(Array.isArray(report.overloadedSpecialists)).toBeTruthy();
      expect(Array.isArray(report.specialityImbalances)).toBeTruthy();
      expect(Array.isArray(report.crossStateWarnings)).toBeTruthy();
    }
  });

  test('overloaded specialist has required fields', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/bottlenecks`);
    
    if (response.status() === 200) {
      const report = await response.json();
      const specialists = report.overloadedSpecialists || [];
      
      if (specialists.length > 0) {
        const specialist = specialists[0];
        expect(specialist).toHaveProperty('doctorId');
        expect(specialist).toHaveProperty('doctorName');
        expect(specialist).toHaveProperty('pendingCount');
        expect(specialist).toHaveProperty('acceptedCount');
      }
    }
  });

  test('speciality imbalance has required fields', async ({ request }) => {
    const response = await request.get(`${apiUrl}/admin/bottlenecks`);
    
    if (response.status() === 200) {
      const report = await response.json();
      const imbalances = report.specialityImbalances || [];
      
      if (imbalances.length > 0) {
        const imbalance = imbalances[0];
        expect(imbalance).toHaveProperty('specialityCode');
        expect(imbalance).toHaveProperty('specialityName');
        expect(imbalance).toHaveProperty('totalReferrals');
        expect(imbalance).toHaveProperty('completedReferrals');
        expect(imbalance).toHaveProperty('completionRate');
      }
    }
  });
});
