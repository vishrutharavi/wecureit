# WeChat Care IT - Comprehensive Testing Guide

## Overview
This document provides a comprehensive guide to running and understanding the test suite for the WeChat Care IT project.

## Test Structure

### 1. **E2E Tests (Playwright)**
Location: `/e2e/tests/`
- **Purpose**: Test entire user workflows from UI to database
- **Coverage**: 25 test cases across 3 suites
- **Files**:
  - `admin-analytics.spec.ts` - Analytics dashboard UI (12 tests)
  - `database-sync.spec.ts` - Data sync integrity (7 tests)
  - `ui-integration.spec.ts` - UI/UX interactions (6 tests)

### 2. **Backend Integration Tests**
Location: `/backend/src/test/java/com/wecureit/integration/`
- **Purpose**: Test API endpoints and database interactions
- **Coverage**: 15 test cases across 3 suites
- **Files**:
  - `AnalyticsIntegrationTest.java` - Analytics API endpoints (15 tests)
  - `Neo4jGraphIntegrationTest.java` - Neo4j graph queries (10 tests)
  - `DatabaseSyncIntegrationTest.java` - PostgreSQL sync (12 tests)

### 3. **Backend Unit Tests**
Location: `/backend/src/test/java/com/wecureit/service/` and `/backend/src/test/java/com/wecureit/repository/`
- **Purpose**: Test individual service and repository classes
- **Coverage**: 18 test cases across 3 suites
- **Files**:
  - `BottleneckAnalyzerServiceTest.java` - Bottleneck analysis logic (10 tests)
  - `CarePathServiceTest.java` - Care path calculations (10 tests)
  - `ReferralRepositoryTest.java` - Database repository operations (8 tests)

## Running Tests

### Running All E2E Tests
```bash
cd e2e
npm install
npm run test
```

### Running Specific E2E Test Suite
```bash
cd e2e
npx playwright test tests/admin-analytics.spec.ts
npx playwright test tests/database-sync.spec.ts
npx playwright test tests/ui-integration.spec.ts
```

### Running UI Inspector (Playwright)
```bash
cd e2e
npx playwright test --ui
```

### Running All Backend Tests
```bash
cd backend
mvn test
```

### Running Specific Backend Test Class
```bash
cd backend
mvn test -Dtest=AnalyticsIntegrationTest
mvn test -Dtest=Neo4jGraphIntegrationTest
mvn test -Dtest=DatabaseSyncIntegrationTest
mvn test -Dtest=BottleneckAnalyzerServiceTest
mvn test -Dtest=CarePathServiceTest
mvn test -Dtest=ReferralRepositoryTest
```

### Running Tests with Coverage
```bash
cd backend
mvn clean test jacoco:report
# Report generated at: target/site/jacoco/index.html
```

## Key Test Scenarios

### E2E Test Scenarios

#### Admin Analytics Tests
1. **Overloaded Specialists Display** - Verifies UI shows doctors with 5+ pending referrals
2. **Speciality Imbalances** - Checks completion rate calculations display correctly
3. **Cross-State Warnings** - Validates cross-state referral alerts appear
4. **Network Graph Rendering** - Confirms Cypher graph visualization loads
5. **Updated Doctor Names** - Verifies manual DB changes sync to UI
6. **No Duplicate Specialities** - Ensures no duplicate CARD nodes cause React warnings
7. **Sync Graph Button** - Tests database sync triggers correctly
8. **Refresh Button** - Validates data refresh from Neo4j
9. **Updated Names in Network** - Confirms network shows Roy, James, etc. (not old test data)
10. **No Old Test Data** - Verifies "Network From", "Network To" nodes removed
11. **Filter Functionality** - Tests doctor filter input
12. **Clear Selection Clearance** - Validates clear button resets state

#### Database Sync Tests
1. **Graph Sync Endpoint** - Checks `/api/admin/intelligence/graph/sync` endpoint
2. **Referral Patterns Retrieval** - Validates `/api/admin/care-path/patterns` returns data
3. **Bottleneck Report Structure** - Verifies response has required fields
4. **Overloaded Specialists Data** - Checks overloaded specialists have valid data
5. **No Duplicate Speciality Codes** - Ensures codes are unique
6. **No Old Test Data Names** - Verifies DOC_NET_FROM, DOC_NET_TO removed
7. **Care Path Patterns Validation** - Checks pattern frequency and doctor names

#### UI Integration Tests
1. **No Console Errors** - Validates page loads without errors
2. **All Tabs Load** - Confirms Alerts, Network, Overview tabs work
3. **No Duplicate Doctor Nodes** - Ensures graph has no duplicate doctors
4. **Refresh Button Works** - Tests data refresh functionality
5. **Sync Button State** - Validates button state during sync
6. **Filter Input** - Tests doctor filter input
7. **Clear Selection** - Validates clear selection button
8. **Data Consistency** - Checks data same across tabs
9. **Full Workflow** - Tests complete admin analytics workflow

### Backend Integration Test Scenarios

#### Analytics API Tests
1. **Bottleneck Report Structure** - Validates JSON response structure
2. **Overloaded Specialists Query** - Tests threshold filtering
3. **Referral Patterns** - Returns care path patterns
4. **No Null Doctor Names** - Validates all names populated
5. **Speciality Imbalance Rates** - Checks completion rates between 0-1
6. **Cross-State Warnings** - Validates patient names present
7. **Referral Patterns Have Names** - Checks doctor names in patterns
8. **No Duplicate Patterns** - Validates unique patterns
9. **Graph Sync Endpoint** - Tests sync trigger
10. **States List** - Returns correct US states

#### Neo4j Graph Tests
1. **Overloaded Specialists Query** - Validates Neo4j Cypher execution
2. **Synced Names** - Checks names synced from PostgreSQL
3. **Speciality Imbalances** - Tests imbalance calculation query
4. **No Duplicate Specialities** - Validates unique speciality codes
5. **Cross-State Warnings** - Returns cross-state referrals
6. **Common Patterns** - Returns frequent referral patterns
7. **No Duplicate Patterns** - Validates unique pattern combinations
8. **No Old Test Data** - Verifies cleaned test data
9. **Patient Sync** - Checks patient names present
10. **Data Completeness** - Validates all analytics queries return data

#### Database Sync Tests
1. **PostgreSQL Doctors Exist** - Validates seed doctors created
2. **PostgreSQL Patients Exist** - Validates seed patients created
3. **PostgreSQL Referrals Exist** - Validates seed referrals created
4. **Doctor Names Synced** - Checks manual edits persisted
5. **Patient Names Synced** - Validates patient name edits
6. **No Duplicate Referrals** - Ensures referral uniqueness
7. **Referral Status Distribution** - Validates status values
8. **Appointments Exist** - Validates seed appointments created
9. **Doctor Licenses Exist** - Validates doctorse have licenses
10. **Facilities Exist** - Validates facility data created
11. **Rooms Exist** - Validates room data created
12. **Doctor Availability Exist** - Validates availability created

### Backend Unit Tests

#### BottleneckAnalyzerService Tests
1. **Threshold Filtering** - Checks pending count >= threshold
2. **Empty Results** - Handles no results gracefully
3. **Data Integrity** - Validates required fields present
4. **Completion Rate Validity** - Checks rates 0 <= rate <= 1
5. **Completion Rate Boundaries** - Tests min/max values
6. **No Null Names** - Validates doctor names not null
7. **No Null IDs** - Validates doctor IDs not null
8. **Non-Negative Count** - Checks pending count >= 0
9. **Valid Speciality Codes** - Ensures CARD/DERM/NEUR/ORTH

#### CarePathService Tests
1. **Patterns Present** - Validates patterns exist
2. **Frequency Valid** - Checks frequency > 1
3. **No Null Doctor Names** - Validates from/to doctor names
4. **No Duplicates** - Checks unique pattern combinations
5. **Frequency Filtering** - Filters patterns by frequency
6. **Speciality Present** - Validates speciality codes
7. **Path Length Valid** - Checks minimum path length
8. **No Self-Referrals** - Validates from != to doctor
9. **Shortest Path Computes** - Tests pathfinding logic

#### ReferralRepository Tests
1. **Doctor Persist** - Validates doctor saves
2. **Find Doctor by UID** - Tests lookup by Firebase UID
3. **Patient Persist** - Validates patient saves
4. **Find Patient by UID** - Tests lookup by Firebase UID
5. **Doctor Name Not Null** - Validates name field
6. **Patient Name Not Null** - Validates name field
7. **Doctor Email Valid** - Validates email format
8. **Patient State Code** - Validates state code
9. **Doctor ID is UUID** - Validates ID type
10. **Patient ID is UUID** - Validates ID type

## Test Data

### Seed Data Characteristics
- **Doctors**: 18 doctors with various specialities (CARD, DERM, NEUR, ORTH)
- **Patients**: 3 patients from different states (CA, TX, FL)
- **Referrals**: 35 referrals across 4 analytics patterns
- **Facilities**: 3 facilities synced to PostgreSQL

### Database Credentials (Test)
- **PostgreSQL**: H2 in-memory test database (automatic)
- **Neo4j**: `bolt://localhost:7687` (testcontainers or local)

### Test Identifiers
- **Doctors**: `firebase_uid LIKE 'seed-ra-doctor-%'`
- **Patients**: `firebase_uid LIKE 'seed-ra-patient-%'`
- **Referrals**: `reason LIKE 'SEED-RA:%'`
- **Tag**: `testTag = 'REALISTIC_SEED_2026_02'`

## Test Results Interpretation

### Passing Tests
- All assertions succeed
- No console errors
- Data consistent across synced layers

### Common Failures

#### ReactKey Warnings (Now Fixed)
- **Cause**: Duplicate speciality codes in Neo4j
- **Fix**: Removed duplicate CARD nodes
- **Status**: ✅ RESOLVED

#### Stale Names in UI
- **Cause**: Cypher script used hardcoded names
- **Fix**: Modified script to dynamically query PostgreSQL
- **Status**: ✅ RESOLVED

#### Old Test Data in Graph
- **Cause**: DOC_NET_FROM, DOC_NET_TO nodes not cleaned
- **Fix**: DETACH DELETE old test scenario nodes
- **Status**: ✅ RESOLVED

#### Duplicate Referrals
- **Cause**: Script ran multiple times without idempotency
- **Fix**: Added ON CONFLICT DO NOTHING to SQL seed
- **Status**: ✅ RESOLVED

## Continuous Integration

### GitHub Actions Workflow
```yaml
on: [push, pull_request]

jobs:
  backend-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
      neo4j:
        image: neo4j:5
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: cd backend && mvn clean test

  frontend-e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: cd frontend && npm install
      - run: npm run build
      - run: cd ../e2e && npm install && npm run test
```

## Performance Benchmarks

### Target Test Execution Times
- **Unit Tests**: < 5 seconds (BottleneckAnalyzerServiceTest, etc.)
- **Repository Tests**: < 10 seconds (ReferralRepositoryTest)
- **Integration Tests**: < 30 seconds (AnalyticsIntegrationTest, DatabaseSyncIntegrationTest)
- **E2E Tests**: < 2 minutes (all Playwright tests combined)

### Total Test Suite Time
- **Backend**: ~45 seconds
- **Frontend E2E**: ~2 minutes
- **Complete Suite**: ~2:45 minutes

## Troubleshooting

### E2E Tests Failing

**Issue**: "Page not loading"
- Ensure Next.js frontend running: `cd frontend && npm run dev`
- Check base URL in `e2e/playwright.config.ts` (default: `http://localhost:3000`)

**Issue**: "Neo4j connection error"
- Ensure Neo4j Docker running: `docker ps | grep neo4j`
- Start if needed: `docker run -d -p 7687:7687 neo4j`

**Issue**: "Database connection timeout"
- Check PostgreSQL connection in `backend/src/main/resources/application.properties`
- Verify Supabase credentials

### Backend Tests Failing

**Issue**: "H2 database errors"
- Clear `target/` directory: `mvn clean`
- Rebuild: `mvn clean install`

**Issue**: "Neo4j mock creation fails"
- Ensure TestConfig.java present in `src/test/java/com/wecureit/config/`
- Check @TestConfiguration annotation

**Issue**: "Tests timeout"
- Increase timeout in application-test.yml
- Check database query performance

## Next Steps

1. **Execute E2E Tests**: Run full Playwright test suite
2. **Execute Backend Tests**: Run all JUnit tests with coverage
3. **Fix Failures**: Address any test failures with targeted fixes
4. **Set Up CI/CD**: Configure GitHub Actions for automated testing
5. **Monitor Coverage**: Track test coverage trends
6. **Expand Tests**: Add more edge cases and error scenarios

## Metrics to Track

- **Test Coverage**: Target > 80% for services
- **Pass Rate**: Target 100% (0 failures)
- **Execution Time**: Track regressions
- **Flakiness**: Monitor test instability
- **Bug Detection**: Tests should catch 90%+ of regressions

## Contact & Support

For test-related issues:
1. Check this guide for troubleshooting
2. Review test logs: `backend/target/surefire-reports/` 
3. Review E2E reports: `e2e/test-results/`
4. Consult team documentation in `/backend/README.md`
