// Realistic referral graph enrichment / fallback graph build
// Purpose:
// 1) ensure seeded doctors/patients/referrals exist in graph even if /graph/sync is unauthorized
// 2) tag seeded referral relationships for easy filtering
// 3) add HAS_SPECIALITY(stateCode) edges needed for cross-state warning query

:param seedTag => 'REALISTIC_SEED_2026_02';

// Ensure core doctor nodes exist
UNWIND [
  {pgId:'10000000-0000-0000-0000-000000000001', name:'From Doctor 1', email:'seed.ra.doctor.from1@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000002', name:'From Doctor 2', email:'seed.ra.doctor.from2@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000003', name:'From Doctor 3', email:'seed.ra.doctor.from3@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000004', name:'From Doctor 4', email:'seed.ra.doctor.from4@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000005', name:'From Doctor 5', email:'seed.ra.doctor.from5@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000006', name:'From Doctor 6', email:'seed.ra.doctor.from6@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000007', name:'From Doctor 7', email:'seed.ra.doctor.from7@wecureit.local'},

  {pgId:'10000000-0000-0000-0000-000000000101', name:'Overloaded 6 Pending', email:'seed.ra.doctor.over6@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000102', name:'Boundary 5 Pending', email:'seed.ra.doctor.over5@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000103', name:'Under Threshold 4 Pending', email:'seed.ra.doctor.over4@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000108', name:'Licensed CA Doctor', email:'seed.ra.doctor.licca@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000109', name:'Missing TX License Doctor', email:'seed.ra.doctor.misstx@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000110', name:'Misc Doctor 1', email:'seed.ra.doctor.misc1@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000111', name:'Misc Doctor 2', email:'seed.ra.doctor.misc2@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000112', name:'Network From', email:'seed.ra.doctor.netfrom@wecureit.local'},
  {pgId:'10000000-0000-0000-0000-000000000113', name:'Network To', email:'seed.ra.doctor.netto@wecureit.local'}
] AS doctorRow
MERGE (d:Doctor {pgId: doctorRow.pgId})
SET d.name = doctorRow.name,
    d.email = doctorRow.email,
    d.isActive = true;

// Ensure patient nodes exist
UNWIND [
  {pgId:'20000000-0000-0000-0000-000000000001', name:'Patient CA 1', stateCode:'CA'},
  {pgId:'20000000-0000-0000-0000-000000000002', name:'Patient TX 1', stateCode:'TX'},
  {pgId:'20000000-0000-0000-0000-000000000003', name:'Patient FL 1', stateCode:'FL'}
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
  testTag: $seedTag,
  createdAt: localdatetime(),
  updatedAt: localdatetime()
}]->(to);

// Tag any referral edges synced from SQL seed by backend sync (if present)
MATCH ()-[r:REFERRED_TO]->()
WHERE r.reason STARTS WITH 'SEED-RA:'
SET r.testTag = $seedTag,
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

// Cleanup prior seeded HAS_SPECIALITY edges for known doctor ids
UNWIND [
  '10000000-0000-0000-0000-000000000101',
  '10000000-0000-0000-0000-000000000102',
  '10000000-0000-0000-0000-000000000103',
  '10000000-0000-0000-0000-000000000108',
  '10000000-0000-0000-0000-000000000109',
  '10000000-0000-0000-0000-000000000110',
  '10000000-0000-0000-0000-000000000111',
  '10000000-0000-0000-0000-000000000113'
] AS doctorPgId
MATCH (d:Doctor {pgId: doctorPgId})-[hs:HAS_SPECIALITY]->(:Speciality)
DELETE hs;

// Recreate seeded license edges (mirrors doctor_license setup)
UNWIND [
  {doctorPgId:'10000000-0000-0000-0000-000000000101', stateCode:'CA', speciality:'CARD'},
  {doctorPgId:'10000000-0000-0000-0000-000000000102', stateCode:'CA', speciality:'CARD'},
  {doctorPgId:'10000000-0000-0000-0000-000000000103', stateCode:'CA', speciality:'CARD'},

  {doctorPgId:'10000000-0000-0000-0000-000000000108', stateCode:'CA', speciality:'CARD'},
  {doctorPgId:'10000000-0000-0000-0000-000000000109', stateCode:'CA', speciality:'CARD'},

  {doctorPgId:'10000000-0000-0000-0000-000000000110', stateCode:'FL', speciality:'DERM'},
  {doctorPgId:'10000000-0000-0000-0000-000000000111', stateCode:'CA', speciality:'CARD'},
  {doctorPgId:'10000000-0000-0000-0000-000000000111', stateCode:'FL', speciality:'ORTH'},

  {doctorPgId:'10000000-0000-0000-0000-000000000113', stateCode:'CA', speciality:'NEUR'}
] AS row
MATCH (d:Doctor {pgId: row.doctorPgId})
MATCH (s:Speciality {code: row.speciality})
MERGE (d)-[hs:HAS_SPECIALITY {stateCode: row.stateCode}]->(s)
SET hs.testTag = $seedTag;

// sanity check
MATCH ()-[r:REFERRED_TO {testTag:$seedTag}]->()
RETURN count(r) AS taggedSeedReferrals;
