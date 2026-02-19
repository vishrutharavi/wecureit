# Quick Testing Commands Reference

## 🚀 Quick Start

### Run Everything
```bash
# Terminal 1: Start backend
cd backend && mvn clean spring-boot:run

# Terminal 2: Start frontend  
cd frontend && npm install && npm run dev

# Terminal 3: Start Neo4j (if using Docker)
docker run -d -p 7687:7687 neo4j

# Terminal 4: Run all E2E tests
cd e2e && npm install && npm run test
```

---

## 📋 Individual Test Execution

### Frontend E2E Tests

**Run all E2E tests:**
```bash
cd e2e
npm run test
```

**Run specific test file:**
```bash
npx playwright test tests/admin-analytics.spec.ts
npx playwright test tests/database-sync.spec.ts
npx playwright test tests/ui-integration.spec.ts
```

**Run single test:**
```bash
npx playwright test -g "overloaded specialists display"
```

**Run with UI (debug mode):**
```bash
npx playwright test --ui
```

**Run with headed browser (see browser):**
```bash
npx playwright test --headed
```

**Generate HTML report:**
```bash
npx playwright test
npx playwright show-report
```

---

### Backend Tests

**Run all backend tests:**
```bash
cd backend
mvn clean test
```

**Run unit tests only:**
```bash
mvn test -DskipITs
```

**Run integration tests only:**
```bash
mvn verify -DskipUTs
```

**Run specific test class:**
```bash
mvn test -Dtest=AnalyticsIntegrationTest
mvn test -Dtest=BottleneckAnalyzerServiceTest
mvn test -Dtest=Neo4jGraphIntegrationTest
mvn test -Dtest=DatabaseSyncIntegrationTest
mvn test -Dtest=CarePathServiceTest
mvn test -Dtest=ReferralRepositoryTest
```

**Run specific test method:**
```bash
mvn test -Dtest=AnalyticsIntegrationTest#testBottleneckReportStructure
```

**Run with verbose output:**
```bash
mvn test -X
```

**Run with coverage:**
```bash
cd backend
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html
```

---

## 🔍 Test Filtering & Selection

### E2E Test Patterns
```bash
# Run tests by tag
npx playwright test --grep @smoke

# Run tests by excluded tag
npx playwright test --grep-invert @slow

# Run in specific browser
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
```

### Backend Test Patterns
```bash
# Run tests by class naming pattern
mvn test -Dtest=*IntegrationTest

# Run tests by annotation (if using @Category)
mvn test -Dgroups=com.wecureit.integration.IntegrationTest

# Run failing tests only
mvn test -DfailIfNoTests=false (then run again)
```

---

## 📊 Test Results & Reporting

### E2E Reports
```bash
cd e2e

# View HTML report
npx playwright show-report

# View specific report format
npx playwright test --reporter=html
npx playwright test --reporter=json > results.json
npx playwright test --reporter=junit > results.xml

# List all tests (without running)
npx playwright test --list
```

### Backend Reports
```bash
cd backend

# View test summary in console
mvn test

# Generate detailed Surefire report
mvn surefire-report:report
# Report at: target/site/surefire-report.html

# Generate coverage report
mvn clean test jacoco:report
# Report at: target/site/jacoco/index.html

# View in browser
open target/site/surefire-report.html
open target/site/jacoco/index.html
```

---

## 🛠️ Debugging Tests

### E2E Debugging
```bash
# Run single test with debugging
npx playwright test tests/admin-analytics.spec.ts --debug

# Generate test trace
npx playwright test --trace on
npx playwright show-trace trace.zip

# Slow down execution
npx playwright test --headed --slow-mo=1000

# Use Playwright Inspector
PWDEBUG=1 npx playwright test
```

### Backend Debugging
```bash
# Run with full stacktrace
mvn test -e

# Run with debug mode (connects to debugger on port 5005)
mvn test -Dmaven.surefire.debug

# View test output files
cat target/surefire-reports/*.txt
cat target/failsafe-reports/*.txt
```

---

## ✅ Data Preparation

### Seed Data Setup
```bash
# Generate seed data in PostgreSQL and Neo4j
cd backend/scripts/seed
./run_realistic_referral_seed.sh

# View seed data in PostgreSQL
psql -h localhost -U wecureit_user -d wecureit
SELECT COUNT(*) FROM doctor WHERE firebase_uid LIKE 'seed-ra-%';
SELECT COUNT(*) FROM referrals WHERE reason LIKE 'SEED-RA:%';

# View seed data in Neo4j
docker exec neo4j cypher-shell -u neo4j -p wecureit_graph \
"MATCH (d:Doctor) RETURN COUNT(d);"

"MATCH (r:REFERRED_TO) RETURN COUNT(r);"
```

### Verify Sync Status
```bash
# Check PostgreSQL doctors
psql -h localhost -U wecureit_user -d wecureit -c \
"SELECT name FROM doctor WHERE firebase_uid LIKE 'seed-ra-doctor-%' ORDER BY name;"

# Check Neo4j doctors
docker exec neo4j cypher-shell -u neo4j -p wecureit_graph \
"MATCH (d:Doctor) RETURN d.name ORDER BY d.name LIMIT 20;"

# Check for duplicates
docker exec neo4j cypher-shell -u neo4j -p wecureit_graph \
"MATCH (s:Speciality) WITH s.code, COUNT(s) as cnt WHERE cnt > 1 RETURN s.code, cnt;"
```

---

## 🔗 Environment Variables

### Add to `.env` or `application.properties`
```properties
# PostgreSQL (for integration tests)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wecureit
SPRING_DATASOURCE_USERNAME=wecureit_user
SPRING_DATASOURCE_PASSWORD=wecureit_password

# Neo4j (for integration tests)
SPRING_NEO4J_URI=bolt://localhost:7687
SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j
SPRING_NEO4J_AUTHENTICATION_PASSWORD=wecureit_graph

# Frontend (for E2E tests)
PLAYWRIGHT_BASE_URL=http://localhost:3000
BROWSER=chromium
HEADLESS=true
```

---

## 📈 CI/CD Pipeline

### Run GitHub Actions Locally
```bash
# Install act (GitHub Actions local executor)
brew install act  # macOS
# or: curl https://raw.githubusercontent.com/nektos/act/master/install.sh | bash

# Run specific workflow
act -j backend-unit-tests

# Run all workflows
act

# View available jobs
act -l
```

### Trigger CI/CD
```bash
# Push to trigger GitHub Actions
git add .
git commit -m "Add tests"
git push origin main

# Check status
git log --oneline | head -5
# Then visit: https://github.com/your-repo/actions
```

---

## 🧹 Clean Up

### Clear Test Artifacts
```bash
# Backend
cd backend
mvn clean

# Frontend/E2E
cd e2e
rm -rf node_modules
rm -rf playwright-report
rm -rf test-results

# Docker/Neo4j
docker stop neo4j
docker rm neo4j
```

### Reset Databases
```bash
# PostgreSQL (keep structure)
psql -h localhost -U wecureit_user -d wecureit -c \
"DELETE FROM referrals WHERE reason LIKE 'SEED-RA:%';
DELETE FROM doctor WHERE firebase_uid LIKE 'seed-ra-%';
DELETE FROM patient WHERE firebase_uid LIKE 'seed-ra-%';"

# Neo4j (completely reset)
docker exec neo4j cypher-shell -u neo4j -p wecureit_graph \
"MATCH (n) DETACH DELETE n;"
```

---

## 📞 Frequently Used Combinations

### Full Test + Coverage
```bash
cd backend && mvn clean test jacoco:report && open target/site/jacoco/index.html
```

### E2E Tests with HTML Report
```bash
cd e2e && npm run test && npx playwright show-report
```

### Single Integration Test with Debugging
```bash
cd backend && mvn test -Dtest=AnalyticsIntegrationTest#testBottleneckReportStructure -e
```

### Run All Tests Sequentially
```bash
cd backend && mvn clean test && cd ../e2e && npm run test
```

### Parallel Test Execution (faster)
```bash
cd backend && mvn test -T 1C  # 1 thread per core
```

---

## 🚨 Common Issues Quick Fixes

**Port already in use:**
```bash
# Backend (8080)
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Frontend (3000)
lsof -i :3000 | grep LISTEN | awk '{print $2}' | xargs kill -9

# Neo4j (7687)
lsof -i :7687 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

**Database connection timeout:**
```bash
# Check if services running
docker ps | grep neo4j
psql -h localhost -U wecureit_user -d wecureit -c "SELECT 1;"

# Restart services
docker restart neo4j
```

**Tests not finding database:**
```bash
# Verify connection strings
grep DATASOURCE backend/src/main/resources/application.properties
grep NEO4J backend/src/main/resources/application.properties
```

---

## 📝 Test Summary Command
```bash
# Show test statistics
echo "=== Backend Tests ===" && \
find backend/src/test -name "*Test.java" | wc -l && \
echo "=== E2E Tests ===" && \
grep -r "test(" e2e/tests/*.spec.ts | wc -l
```

**Output should show:**
- 6 Backend test files
- 25 E2E test cases
- **Total: 90+ test cases**

---

## 🎯 Success Checklist

Before committing:
- [ ] `mvn clean test` passes in backend
- [ ] `npm run test` passes in e2e
- [ ] No console errors in browser
- [ ] No duplicate key warnings in React
- [ ] All doctor names are synced
- [ ] No old test data particles visible

---

**For detailed information, see**: `TESTING_GUIDE.md`
