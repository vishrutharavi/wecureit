#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
SQL_FILE="$SCRIPT_DIR/realistic_referral_seed.sql"
CYPHER_FILE="$BACKEND_DIR/scripts/neo4j/realistic_referral_graph_enrichment.cypher"

BACKEND_BASE_URL="${BACKEND_BASE_URL:-http://localhost:8080}"
ADMIN_BEARER_TOKEN="${ADMIN_BEARER_TOKEN:-}"
NEO4J_CONTAINER="${NEO4J_CONTAINER:-neo4j}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-wecureit_graph}"

build_db_url_from_properties() {
  local props_file="$BACKEND_DIR/src/main/resources/application.properties"
  if [[ ! -f "$props_file" ]]; then
    return 1
  fi

  local jdbc_url db_user db_pass hostport dbname
  jdbc_url="$(grep -E '^spring.datasource.url=' "$props_file" | head -n1 | cut -d'=' -f2-)"
  db_user="$(grep -E '^spring.datasource.username=' "$props_file" | head -n1 | cut -d'=' -f2-)"
  db_pass="$(grep -E '^spring.datasource.password=' "$props_file" | head -n1 | cut -d'=' -f2-)"

  if [[ -z "$jdbc_url" || -z "$db_user" || -z "$db_pass" ]]; then
    return 1
  fi

  if [[ "$jdbc_url" != jdbc:postgresql://* ]]; then
    return 1
  fi

  hostport="${jdbc_url#jdbc:postgresql://}"
  dbname="${hostport#*/}"
  hostport="${hostport%%/*}"

  printf 'postgresql://%s:%s@%s/%s' "$db_user" "$db_pass" "$hostport" "$dbname"
}

DB_URL="${DATABASE_URL:-}"
if [[ -z "$DB_URL" ]]; then
  DB_URL="$(build_db_url_from_properties || true)"
fi

if [[ -z "$DB_URL" ]]; then
  echo "[ERROR] Could not resolve DB URL. Set DATABASE_URL, or keep datasource values in backend/src/main/resources/application.properties"
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "[ERROR] psql not found. Install PostgreSQL client tools and retry."
  exit 1
fi

echo "[1/4] Seeding relational data (users, appointments, referrals) into PostgreSQL..."
psql -v ON_ERROR_STOP=1 "$DB_URL" -f "$SQL_FILE"

echo "[2/4] Triggering graph full sync from backend (referrals -> Neo4j)..."
if [[ -n "$ADMIN_BEARER_TOKEN" ]]; then
  sync_cmd=(curl -fsS -X POST -H "Authorization: Bearer $ADMIN_BEARER_TOKEN" "$BACKEND_BASE_URL/api/admin/intelligence/graph/sync")
else
  sync_cmd=(curl -fsS -X POST "$BACKEND_BASE_URL/api/admin/intelligence/graph/sync")
fi

if "${sync_cmd[@]}" >/dev/null; then
  echo "Graph sync endpoint returned success."
else
  echo "[WARN] Graph sync endpoint failed (backend may be down or endpoint may be protected)."
  echo "       Start backend and call: POST $BACKEND_BASE_URL/api/admin/intelligence/graph/sync"
  echo "       If protected, export ADMIN_BEARER_TOKEN and rerun."
fi

echo "[3/4] Applying Neo4j enrichment (tag referrals + HAS_SPECIALITY edges)..."
echo "       Generating Cypher with current PostgreSQL doctor/patient names..."

# Query PostgreSQL for current seed doctors and generate Cypher UNWIND array
doctor_cypher=$(psql -qtA "$DB_URL" <<'EOSQL'
SELECT string_agg(
  format('  {pgId:%L, name:%L, email:%L}', 
    id::text, 
    name, 
    email
  ),
  E',\n'
  ORDER BY id
)
FROM doctor 
WHERE firebase_uid LIKE 'seed-ra-doctor-%';
EOSQL
)

# Query PostgreSQL for current seed patients and generate Cypher UNWIND array
patient_cypher=$(psql -qtA "$DB_URL" <<'EOSQL'
SELECT string_agg(
  format('  {pgId:%L, name:%L, stateCode:%L}', 
    id::text, 
    name, 
    COALESCE(state_code, 'UNKNOWN')
  ),
  E',\n'
  ORDER BY id
)
FROM patient 
WHERE firebase_uid LIKE 'seed-ra-patient-%';
EOSQL
)

# Generate complete Cypher script with dynamic doctor/patient data
cat <<EOCYPHER | docker exec -i "$NEO4J_CONTAINER" cypher-shell -u "$NEO4J_USER" -p "$NEO4J_PASSWORD"
// Realistic referral graph enrichment - dynamically generated from PostgreSQL
:param seedTag => 'REALISTIC_SEED_2026_02';

// Ensure core doctor nodes exist (with current PostgreSQL names)
UNWIND [
$doctor_cypher
] AS doctorRow
MERGE (d:Doctor {pgId: doctorRow.pgId})
SET d.name = doctorRow.name,
    d.email = doctorRow.email,
    d.isActive = true;

// Ensure patient nodes exist (with current PostgreSQL names)
UNWIND [
$patient_cypher
] AS patientRow
MERGE (p:Patient {pgId: patientRow.pgId})
SET p.name = patientRow.name,
    p.stateCode = patientRow.stateCode;

// Remove prior seeded referral edges and recreate deterministic set
MATCH ()-[r:REFERRED_TO]->()
WHERE r.reason STARTS WITH 'SEED-RA:'
DELETE r;

UNWIND [
  {referralId:'70000000-0000-0000-0000-000000000001', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000101', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER6 #1'},
  {referralId:'70000000-0000-0000-0000-000000000002', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000101', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER6 #2'},
  {referralId:'70000000-0000-0000-0000-000000000003', from:'10000000-0000-0000-0000-000000000003', to:'10000000-0000-0000-0000-000000000101', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER6 #3'},
  {referralId:'70000000-0000-0000-0000-000000000004', from:'10000000-0000-0000-0000-000000000004', to:'10000000-0000-0000-0000-000000000101', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER6 #4'},
  {referralId:'70000000-0000-0000-0000-000000000005', from:'10000000-0000-0000-0000-000000000005', to:'10000000-0000-0000-0000-000000000101', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER6 #5'},
  {referralId:'70000000-0000-0000-0000-000000000006', from:'10000000-0000-0000-0000-000000000006', to:'10000000-0000-0000-0000-000000000101', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER6 #6'},

  {referralId:'70000000-0000-0000-0000-000000000007', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000102', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER5 #1'},
  {referralId:'70000000-0000-0000-0000-000000000008', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000102', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER5 #2'},
  {referralId:'70000000-0000-0000-0000-000000000009', from:'10000000-0000-0000-0000-000000000003', to:'10000000-0000-0000-0000-000000000102', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER5 #3'},
  {referralId:'70000000-0000-0000-0000-000000000010', from:'10000000-0000-0000-0000-000000000004', to:'10000000-0000-0000-0000-000000000102', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER5 #4'},
  {referralId:'70000000-0000-0000-0000-000000000011', from:'10000000-0000-0000-0000-000000000005', to:'10000000-0000-0000-0000-000000000102', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER5 #5'},

  {referralId:'70000000-0000-0000-0000-000000000012', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000103', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER4 P #1'},
  {referralId:'70000000-0000-0000-0000-000000000013', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000103', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER4 P #2'},
  {referralId:'70000000-0000-0000-0000-000000000014', from:'10000000-0000-0000-0000-000000000003', to:'10000000-0000-0000-0000-000000000103', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER4 P #3'},
  {referralId:'70000000-0000-0000-0000-000000000015', from:'10000000-0000-0000-0000-000000000004', to:'10000000-0000-0000-0000-000000000103', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: OVER4 P #4'},
  {referralId:'70000000-0000-0000-0000-000000000016', from:'10000000-0000-0000-0000-000000000006', to:'10000000-0000-0000-0000-000000000103', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'COMPLETED', reason:'SEED-RA: OVER4 C #1'},
  {referralId:'70000000-0000-0000-0000-000000000017', from:'10000000-0000-0000-0000-000000000007', to:'10000000-0000-0000-0000-000000000103', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'COMPLETED', reason:'SEED-RA: OVER4 C #2'},

  {referralId:'70000000-0000-0000-0000-000000000018', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000110', patient:'20000000-0000-0000-0000-000000000003', speciality:'DERM', status:'PENDING', reason:'SEED-RA: DERM P #1'},
  {referralId:'70000000-0000-0000-0000-000000000019', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000110', patient:'20000000-0000-0000-0000-000000000003', speciality:'DERM', status:'PENDING', reason:'SEED-RA: DERM P #2'},
  {referralId:'70000000-0000-0000-0000-000000000020', from:'10000000-0000-0000-0000-000000000003', to:'10000000-0000-0000-0000-000000000110', patient:'20000000-0000-0000-0000-000000000003', speciality:'DERM', status:'PENDING', reason:'SEED-RA: DERM P #3'},
  {referralId:'70000000-0000-0000-0000-000000000021', from:'10000000-0000-0000-0000-000000000004', to:'10000000-0000-0000-0000-000000000110', patient:'20000000-0000-0000-0000-000000000003', speciality:'DERM', status:'PENDING', reason:'SEED-RA: DERM P #4'},
  {referralId:'70000000-0000-0000-0000-000000000022', from:'10000000-0000-0000-0000-000000000005', to:'10000000-0000-0000-0000-000000000110', patient:'20000000-0000-0000-0000-000000000003', speciality:'DERM', status:'COMPLETED', reason:'SEED-RA: DERM C #1'},

  {referralId:'70000000-0000-0000-0000-000000000023', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'COMPLETED', reason:'SEED-RA: CARD C #1'},
  {referralId:'70000000-0000-0000-0000-000000000024', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'COMPLETED', reason:'SEED-RA: CARD C #2'},
  {referralId:'70000000-0000-0000-0000-000000000025', from:'10000000-0000-0000-0000-000000000003', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'COMPLETED', reason:'SEED-RA: CARD C #3'},
  {referralId:'70000000-0000-0000-0000-000000000026', from:'10000000-0000-0000-0000-000000000004', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'COMPLETED', reason:'SEED-RA: CARD C #4'},
  {referralId:'70000000-0000-0000-0000-000000000027', from:'10000000-0000-0000-0000-000000000005', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: CARD P #1'},

  {referralId:'70000000-0000-0000-0000-000000000028', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000003', speciality:'ORTH', status:'ACCEPTED', reason:'SEED-RA: ORTH A #1'},
  {referralId:'70000000-0000-0000-0000-000000000029', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000003', speciality:'ORTH', status:'ACCEPTED', reason:'SEED-RA: ORTH A #2'},
  {referralId:'70000000-0000-0000-0000-000000000030', from:'10000000-0000-0000-0000-000000000003', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000003', speciality:'ORTH', status:'ACCEPTED', reason:'SEED-RA: ORTH A #3'},

  {referralId:'70000000-0000-0000-0000-000000000031', from:'10000000-0000-0000-0000-000000000001', to:'10000000-0000-0000-0000-000000000109', patient:'20000000-0000-0000-0000-000000000002', speciality:'CARD', status:'PENDING', reason:'SEED-RA: XSTATE WARN #1'},
  {referralId:'70000000-0000-0000-0000-000000000032', from:'10000000-0000-0000-0000-000000000002', to:'10000000-0000-0000-0000-000000000108', patient:'20000000-0000-0000-0000-000000000001', speciality:'CARD', status:'PENDING', reason:'SEED-RA: XSTATE OK #1'},

  {referralId:'70000000-0000-0000-0000-000000000033', from:'10000000-0000-0000-0000-000000000112', to:'10000000-0000-0000-0000-000000000113', patient:'20000000-0000-0000-0000-000000000001', speciality:'NEUR', status:'PENDING', reason:'SEED-RA: NET DUP #1'},
  {referralId:'70000000-0000-0000-0000-000000000034', from:'10000000-0000-0000-0000-000000000112', to:'10000000-0000-0000-0000-000000000113', patient:'20000000-0000-0000-0000-000000000001', speciality:'NEUR', status:'PENDING', reason:'SEED-RA: NET DUP #2'},
  {referralId:'70000000-0000-0000-0000-000000000035', from:'10000000-0000-0000-0000-000000000112', to:'10000000-0000-0000-0000-000000000111', patient:'20000000-0000-0000-0000-000000000001', speciality:'NEUR', status:'PENDING', reason:'SEED-RA: NET SINGLE #1'}
] AS rr
MATCH (from:Doctor {pgId: rr.from})
MATCH (to:Doctor {pgId: rr.to})
CREATE (from)-[:REFERRED_TO {
  referralId: rr.referralId,
  patientPgId: rr.patient,
  specialityCode: rr.speciality,
  status: rr.status,
  reason: rr.reason,
  testTag: \$seedTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// Tag any referral edges synced from SQL seed by backend sync (if present)
MATCH ()-[r:REFERRED_TO]->()
WHERE r.reason STARTS WITH 'SEED-RA:'
SET r.testTag = \$seedTag,
    r.updatedAt = localdatetime();

// Ensure speciality nodes exist
UNWIND [
  {code:'CARD', name:'Cardiology'},
  {code:'DERM', name:'Dermatology'},
  {code:'NEUR', name:'Neurology'},
  {code:'ORTH', name:'Orthopedics'}
] AS spec
MERGE (s:Speciality {code: spec.code})
SET s.name = spec.name;

// Add HAS_SPECIALITY edges for cross-state license checking
MATCH (d:Doctor {pgId: '10000000-0000-0000-0000-000000000108'})
MERGE (d)-[:HAS_SPECIALITY {stateCode: 'CA'}]->(:Speciality {code: 'CARD'});

MATCH (d:Doctor {pgId: '10000000-0000-0000-0000-000000000109'})
MERGE (d)-[:HAS_SPECIALITY {stateCode: 'FL'}]->(:Speciality {code: 'CARD'});
EOCYPHER

echo "[4/4] Quick validation (seeded referral counts by status)..."
docker exec -i "$NEO4J_CONTAINER" cypher-shell -u "$NEO4J_USER" -p "$NEO4J_PASSWORD" \
  "MATCH ()-[r:REFERRED_TO {testTag:'REALISTIC_SEED_2026_02'}]->() RETURN r.status AS status, count(*) AS count ORDER BY count DESC;"

echo "Done. You can now run your analytics queries with WHERE r.testTag = 'REALISTIC_SEED_2026_02'."
