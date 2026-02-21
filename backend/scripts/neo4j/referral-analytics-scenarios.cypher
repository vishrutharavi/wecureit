// Referral analytics test scenarios for Neo4j
// Safe to re-run: clears only records tagged with testTag = 'REF_ANALYTICS_2026_02'

:param testTag => 'REF_ANALYTICS_2026_02';

// 0) Cleanup prior seeded data for this scenario pack
MATCH ()-[r:REFERRED_TO {testTag: $testTag}]->() DELETE r;
MATCH ()-[r:HAS_SPECIALITY {testTag: $testTag}]->() DELETE r;
MATCH (n) WHERE n.testTag = $testTag DETACH DELETE n;

// 1) Seed Doctors
UNWIND [
  {pgId:'DOC_FROM_1', name:'From Doctor 1'},
  {pgId:'DOC_FROM_2', name:'From Doctor 2'},
  {pgId:'DOC_FROM_3', name:'From Doctor 3'},
  {pgId:'DOC_FROM_4', name:'From Doctor 4'},
  {pgId:'DOC_FROM_5', name:'From Doctor 5'},
  {pgId:'DOC_FROM_6', name:'From Doctor 6'},
  {pgId:'DOC_FROM_7', name:'From Doctor 7'},

  {pgId:'DOC_OVER_6', name:'Overloaded 6 Pending'},
  {pgId:'DOC_OVER_5', name:'Boundary 5 Pending'},
  {pgId:'DOC_OVER_4', name:'Under Threshold 4 Pending'},

  {pgId:'DOC_NET_FROM', name:'Network From'},
  {pgId:'DOC_NET_TO', name:'Network To'},

  {pgId:'DOC_LIC_CA', name:'Licensed CA Doctor'},
  {pgId:'DOC_MISS_TX', name:'Missing TX License Doctor'},

  {pgId:'DOC_MISC_1', name:'Misc Doctor 1'},
  {pgId:'DOC_MISC_2', name:'Misc Doctor 2'}
] AS doc
MERGE (d:Doctor {pgId: doc.pgId})
SET d.name = doc.name,
    d.email = toLower(replace(doc.pgId, '_', '.')) + '@test.local',
    d.isActive = true,
    d.testTag = $testTag;

// 2) Seed Patients
UNWIND [
  {pgId:'PAT_CA_1', name:'Patient CA 1', stateCode:'CA'},
  {pgId:'PAT_TX_1', name:'Patient TX 1', stateCode:'TX'},
  {pgId:'PAT_FL_1', name:'Patient FL 1', stateCode:'FL'}
] AS pat
MERGE (p:Patient {pgId: pat.pgId})
SET p.name = pat.name,
    p.stateCode = pat.stateCode,
    p.testTag = $testTag;

// 3) Seed Speciality nodes (optional but useful for readability)
UNWIND [
  {code:'CARD', name:'Cardiology'},
  {code:'DERM', name:'Dermatology'},
  {code:'NEUR', name:'Neurology'},
  {code:'ORTHO', name:'Orthopedics'}
] AS spec
MERGE (s:Speciality {code: spec.code})
SET s.name = spec.name,
    s.testTag = $testTag;

// 4) Seed doctor licence graph edges for Cross-State warning checks
// Query under test expects: (to)-[:HAS_SPECIALITY {stateCode: <patientState>}]->()
MATCH (dLicCa:Doctor {pgId:'DOC_LIC_CA'}), (sCard:Speciality {code:'CARD'})
MERGE (dLicCa)-[r1:HAS_SPECIALITY {stateCode:'CA'}]->(sCard)
SET r1.testTag = $testTag;

MATCH (dMissTx:Doctor {pgId:'DOC_MISS_TX'}), (sCard:Speciality {code:'CARD'})
MERGE (dMissTx)-[r2:HAS_SPECIALITY {stateCode:'CA'}]->(sCard)
SET r2.testTag = $testTag;

UNWIND ['DOC_OVER_6','DOC_OVER_5','DOC_OVER_4','DOC_NET_TO','DOC_MISC_2'] AS docId
MATCH (d:Doctor {pgId: docId}), (sCard:Speciality {code:'CARD'})
MERGE (d)-[r:HAS_SPECIALITY {stateCode:'CA'}]->(sCard)
SET r.testTag = $testTag;

MATCH (dMisc1:Doctor {pgId:'DOC_MISC_1'}), (sCard:Speciality {code:'CARD'})
MERGE (dMisc1)-[r3:HAS_SPECIALITY {stateCode:'CA'}]->(sCard)
SET r3.testTag = $testTag;
MATCH (dMisc1:Doctor {pgId:'DOC_MISC_1'}), (sCard:Speciality {code:'CARD'})
MERGE (dMisc1)-[r4:HAS_SPECIALITY {stateCode:'FL'}]->(sCard)
SET r4.testTag = $testTag;

// 5) Seed referral edges
// 5A) Overloaded Specialists scenarios
UNWIND [1,2,3,4,5,6] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_OVER_6'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_OVER6_' + toString(i),
  patientPgId: 'PAT_CA_1',
  specialityCode: 'OV6_' + toString(i),
  status: 'PENDING',
  reason: 'Overloaded test 6',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

UNWIND [1,2,3,4,5] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_OVER_5'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_OVER5_' + toString(i),
  patientPgId: 'PAT_CA_1',
  specialityCode: 'OV5_' + toString(i),
  status: 'PENDING',
  reason: 'Overloaded boundary test 5',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

UNWIND [1,2,3,4] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_OVER_4'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_OVER4_P_' + toString(i),
  patientPgId: 'PAT_CA_1',
  specialityCode: 'OV4P_' + toString(i),
  status: 'PENDING',
  reason: 'Under threshold pending 4',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

UNWIND [1,2] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_OVER_4'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_OVER4_C_' + toString(i),
  patientPgId: 'PAT_CA_1',
  specialityCode: 'OV4C_' + toString(i),
  status: 'COMPLETED',
  reason: 'Under threshold completed noise',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// 5B) Speciality Imbalance scenarios
// DERM => total 5, completed 1 (low completion)
UNWIND [1,2,3,4] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_MISC_1'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_DERM_P_' + toString(i),
  patientPgId: 'PAT_FL_1',
  specialityCode: 'DERM',
  status: 'PENDING',
  reason: 'Derm low completion pending',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

MATCH (from:Doctor {pgId:'DOC_FROM_5'}), (to:Doctor {pgId:'DOC_MISC_1'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_DERM_C_1',
  patientPgId: 'PAT_FL_1',
  specialityCode: 'DERM',
  status: 'COMPLETED',
  reason: 'Derm low completion completed one',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// CARD => total 5, completed 4 (healthy completion)
UNWIND [1,2,3,4] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_MISC_2'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_CARD_C_' + toString(i),
  patientPgId: 'PAT_CA_1',
  specialityCode: 'CARD',
  status: 'COMPLETED',
  reason: 'Card healthy completion',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

MATCH (from:Doctor {pgId:'DOC_FROM_5'}), (to:Doctor {pgId:'DOC_MISC_2'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_CARD_P_1',
  patientPgId: 'PAT_CA_1',
  specialityCode: 'CARD',
  status: 'PENDING',
  reason: 'Card healthy completion pending one',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// ORTHO => total 3 (boundary excluded by total > 3)
UNWIND [1,2,3] AS i
MATCH (from:Doctor {pgId:'DOC_FROM_' + toString(i)}), (to:Doctor {pgId:'DOC_MISC_1'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_ORTHO_' + toString(i),
  patientPgId: 'PAT_FL_1',
  specialityCode: 'ORTHO',
  status: 'ACCEPTED',
  reason: 'Ortho total boundary',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// 5C) Cross-State warnings
// Warning case: patient in TX, to-doctor only has CA
MATCH (from:Doctor {pgId:'DOC_FROM_1'}), (to:Doctor {pgId:'DOC_MISS_TX'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_XSTATE_WARN_1',
  patientPgId: 'PAT_TX_1',
  specialityCode: 'CARD',
  status: 'PENDING',
  reason: 'Cross-state warning expected',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// Non-warning case: patient in CA, to-doctor has CA
MATCH (from:Doctor {pgId:'DOC_FROM_2'}), (to:Doctor {pgId:'DOC_LIC_CA'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_XSTATE_OK_1',
  patientPgId: 'PAT_CA_1',
  specialityCode: 'CARD',
  status: 'PENDING',
  reason: 'Cross-state not expected',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// 5D) Network graph pair frequency > 1
UNWIND [1,2] AS i
MATCH (from:Doctor {pgId:'DOC_NET_FROM'}), (to:Doctor {pgId:'DOC_NET_TO'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_NET_DUP_' + toString(i),
  patientPgId: 'PAT_CA_1',
  specialityCode: 'NEUR',
  status: 'PENDING',
  reason: 'Network duplicate pair',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

MATCH (from:Doctor {pgId:'DOC_NET_FROM'}), (to:Doctor {pgId:'DOC_MISC_2'})
CREATE (from)-[:REFERRED_TO {
  referralId: 'R_NET_SINGLE_1',
  patientPgId: 'PAT_CA_1',
  specialityCode: 'NEUR',
  status: 'PENDING',
  reason: 'Network single edge control',
  testTag: $testTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// 6) Quick sanity counters
MATCH ()-[r:REFERRED_TO {testTag: $testTag}]->()
RETURN count(r) AS seededReferralEdges;
