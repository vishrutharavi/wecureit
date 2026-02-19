# wecureit backend

This is a minimal Spring Boot Maven project skeleton generated into `backend/`.

Quick start

1. Ensure you have a JDK 21 installed.
2. From repository root:

```bash
cd backend
mvn spring-boot:run
```

If you prefer the Maven wrapper, you can generate it by running `mvn -N io.takari:maven:wrapper` or download the wrapper manually.

Update `src/main/resources/application.properties` with your PostgreSQL credentials before connecting to a DB.

## Neo4j referral analytics test scenarios

Use this script to seed reusable test data for:

- Overloaded Specialists
- Speciality Imbalances
- Cross-State Warnings
- Network Graph pair frequency

Script path:

`scripts/neo4j/referral-analytics-scenarios.cypher`

### Run in Neo4j Browser

1. Open `http://localhost:7474`
2. Login with your Neo4j credentials from `application.properties`
3. Copy/paste and run the script file contents.

The script is idempotent for its own tagged data (`testTag = REF_ANALYTICS_2026_02`) and starts by cleaning previously seeded records with that tag.

### Validation queries

#### 1) Overloaded Specialists (>=5 pending incoming)

```cypher
MATCH (d:Doctor)<-[r:REFERRED_TO]-(:Doctor)
WHERE r.testTag = 'REF_ANALYTICS_2026_02'
WITH d, sum(CASE WHEN r.status = 'PENDING' THEN 1 ELSE 0 END) AS pendingCount
WHERE pendingCount >= 5
RETURN d.pgId, d.name, pendingCount
ORDER BY pendingCount DESC;
```

Expected: `DOC_OVER_6` and `DOC_OVER_5` appear.

#### 2) Speciality Imbalances (total > 3 and low completion)

```cypher
MATCH ()-[r:REFERRED_TO]->()
WHERE r.testTag = 'REF_ANALYTICS_2026_02'
WITH r.specialityCode AS specialityCode,
		 count(r) AS total,
		 sum(CASE WHEN r.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed
WITH specialityCode, total, completed,
		 CASE WHEN total = 0 THEN 0.0 ELSE (1.0 * completed / total) END AS completionRate
WHERE total > 3 AND completionRate < 0.4
RETURN specialityCode, total, completed, completionRate
ORDER BY total DESC;
```

Expected: `DERM` appears; `CARD` should not.

#### 3) Cross-State Warnings

```cypher
MATCH (from:Doctor)-[r:REFERRED_TO]->(to:Doctor)
MATCH (p:Patient {pgId: r.patientPgId})
WHERE r.testTag = 'REF_ANALYTICS_2026_02'
	AND NOT EXISTS {
		MATCH (to)-[:HAS_SPECIALITY {stateCode: p.stateCode}]->()
	}
RETURN from.pgId AS fromDoctor, to.pgId AS toDoctor, p.pgId AS patient, p.stateCode AS patientState, r.referralId
ORDER BY r.referralId;
```

Expected: includes `R_XSTATE_WARN_1` and excludes `R_XSTATE_OK_1`.

#### 4) Network Graph patterns (>1 referral between doctor pairs by speciality)

```cypher
MATCH (from:Doctor)-[r:REFERRED_TO]->(to:Doctor)
WHERE r.testTag = 'REF_ANALYTICS_2026_02'
WITH from.name AS fromDoctor, to.name AS toDoctor, r.specialityCode AS specialityCode, count(r) AS frequency
WHERE frequency > 1
RETURN fromDoctor, toDoctor, specialityCode, frequency
ORDER BY frequency DESC, fromDoctor, toDoctor;
```

Expected: pair `DOC_NET_FROM -> DOC_NET_TO` with `NEUR` appears with frequency `2`.

## Realistic seed (users + appointments + referrals)

If you want closer-to-real test data (PostgreSQL first, then Neo4j sync), use:

- SQL seed: `scripts/seed/realistic_referral_seed.sql`
- Graph enrichment: `scripts/neo4j/realistic_referral_graph_enrichment.cypher`
- One-command runner: `scripts/seed/run_realistic_referral_seed.sh`

### What it creates

- Doctors and patients with stable UUIDs
- Facilities, rooms, and doctor availability
- Appointments linked to those users
- Referrals across statuses (`PENDING`, `COMPLETED`, `ACCEPTED`)
- Doctor license records in PostgreSQL
- Neo4j `HAS_SPECIALITY` edges required for cross-state warning checks

### Run

From repository root:

```bash
cd backend
./scripts/seed/run_realistic_referral_seed.sh
```

Notes:

- Runner tries to resolve DB connection from `src/main/resources/application.properties` unless `DATABASE_URL` is provided.
- Ensure backend is running before sync call (`POST /api/admin/intelligence/graph/sync`).
- If admin endpoints are protected, set `ADMIN_BEARER_TOKEN` before running.
- Neo4j container defaults to `neo4j` with password `wecureit_graph`; override via env vars if needed.

### Updating seed data manually

The seed script **preserves manual edits** to doctor and patient names in PostgreSQL:

1. **First run**: Creates seed doctors/patients with default names
2. **Manual edits**: Change names directly in PostgreSQL - they won't be overwritten
3. **Sync to UI**: Use the UI buttons to sync PostgreSQL → Neo4j → Frontend

**Complete workflow to see manual PostgreSQL changes in the Analytics UI:**

#### Step 1: Edit names in PostgreSQL
Change doctor/patient names in the database as needed. The seed script won't overwrite them.

#### Step 2: Sync PostgreSQL → Neo4j → Frontend UI

**Option A - Use the Admin UI Buttons** (✨ **RECOMMENDED** - easiest):
1. Go to **Admin → Analytics → Alerts** or **Network** tab
2. Click the **"Sync Graph"** button (red gradient button)
   - This calls the backend endpoint to sync PostgreSQL → Neo4j
   - Wait for "Syncing..." to complete
3. Click **"Refresh"** button (white button with red text)
   - This fetches the latest data from Neo4j to the frontend
4. ✅ Your manual name changes now appear in the UI!

**Option B - Re-run the seed script** (development/testing):
```bash
./scripts/seed/run_realistic_referral_seed.sh
```
- Preserves your manual edits (ON CONFLICT DO NOTHING)
- Reads current PostgreSQL names and updates Neo4j
- Then refresh the browser tab to see updated names

**Option C - Call the sync endpoint directly** (automation/scripts):
```bash
curl -X POST http://localhost:8080/api/admin/intelligence/graph/sync \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```
Then refresh the browser tab.

> **💡 How it works:** The Alerts and Network tabs query Neo4j (not PostgreSQL) for performance. After changing names in PostgreSQL, you must sync to Neo4j using "Sync Graph" button or the seed script, then click "Refresh" to reload the UI data.

> **✅ Current sync status:** All 18 doctors + 3 patients synced successfully!

### Filter analytics to this seed set

Use this in Neo4j queries:

```cypher
WHERE r.testTag = 'REALISTIC_SEED_2026_02'
```
