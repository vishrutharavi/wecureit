import { apiFetch } from "../api";

/* ---------- Facilities ---------- */

export async function createFacility(token: string, payload: {
  name: string;
  address: string;
  city: string;
  stateCode: string;
  zipCode: string;
  phone?: string;
  email?: string;
}) {
  return apiFetch("/api/admin/facilities", token, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getFacilities(token: string) {
  return apiFetch("/api/admin/facilities", token);
}

export async function updateFacility(token: string, facilityId: string, payload: {
  name?: string;
  address?: string;
  city?: string;
  stateCode?: string;
  zipCode?: string;
}) {
  return apiFetch(`/api/admin/facilities/updateFacility/${facilityId}`, token, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deactivateFacility(token: string, facilityId: string) {
  return apiFetch(`/api/admin/facilities/${facilityId}/delete`, token, {
    method: "PATCH",
  });
}

export async function createRoom(
  token: string,
  facilityId: string,
  payload: {
    roomNumber: string;
    specialityCode: string;
  }
) {
  // backend expects POST /api/admin/rooms with facilityId in the body
  const body = { facilityId, roomNumber: payload.roomNumber, specialityCode: payload.specialityCode };
  return apiFetch(`/api/admin/rooms`, token, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function updateRoom(
  token: string,
  roomId: string,
  payload: {
    roomNumber?: string;
    specialityCode?: string;
  }
) {
  return apiFetch(`/api/admin/rooms/${roomId}`, token, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deactivateRoom(token: string, roomId: string) {
  return apiFetch(`/api/admin/rooms/${roomId}/deactivate`, token, {
    method: "PATCH",
  });
}

/* ---------- Doctors ---------- */

export async function getDoctors(token: string) {
  return apiFetch("/api/admin/doctors", token);
}

export async function createDoctor(token: string, payload: {
  email: string;
  name: string;
  gender?: string;
  password: string;
}) {
  return apiFetch("/api/admin/doctors", token, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function addDoctorLicense(
  token: string,
  doctorId: string,
  payload: {
    stateCode: string;
    specialityCode: string;
  }
) {
  // backend exposes a single endpoint for adding doctor licenses
  // POST /api/admin/doctor-licenses { doctorId, stateCode, specialityCode }
  return apiFetch(`/api/admin/doctor-licenses`, token, {
    method: "POST",
    body: JSON.stringify({ doctorId, stateCode: payload.stateCode, specialityCode: payload.specialityCode }),
  });
}

export async function getDoctorLicenses(token: string, doctorId: string) {
  return apiFetch(`/api/admin/doctor-licenses?doctorId=${doctorId}`, token);
}

export async function deactivateDoctor(token: string, doctorId: string) {
  return apiFetch(`/api/admin/doctors/${doctorId}/delete`, token, {
    method: "PATCH",
  });
}

/* ---------- Meta (States & Specialities) ---------- */

export async function getStates(token: string) {
  return apiFetch("/api/admin/states", token);
}

export async function getSpecialities(token: string) {
  return apiFetch("/api/admin/specialities", token);
}

/* ---------- Doctor Update ---------- */

export async function updateDoctor(
  token: string,
  doctorId: string,
  payload: { name?: string; gender?: string }
) {
  return apiFetch(`/api/admin/doctors/${doctorId}/update`, token, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/* ---------- Doctor License Deactivation ---------- */

export async function deactivateDoctorLicense(token: string, licenseId: string) {
  return apiFetch(`/api/admin/doctor-licenses/${licenseId}/deactivate`, token, {
    method: "PATCH",
  });
}

/* ---------- Referral Analytics ---------- */

export async function getReferralOverview(token: string) {
  return apiFetch("/api/admin/referrals/stats/overview", token);
}

export async function getReferralTrends(token: string, days = 30) {
  return apiFetch(`/api/admin/referrals/stats/trends?days=${days}`, token);
}

export async function getReferralsByDoctor(token: string) {
  return apiFetch("/api/admin/referrals/stats/by-doctor", token);
}

export async function getReferralsBySpeciality(token: string) {
  return apiFetch("/api/admin/referrals/stats/by-speciality", token);
}

/* ---------- Intelligence (Neo4j) ---------- */

export async function getBottleneckReport(token: string) {
  return apiFetch("/api/admin/intelligence/bottlenecks", token);
}

export async function getOverloadedSpecialists(token: string, threshold = 1) {
  return apiFetch(`/api/admin/intelligence/bottlenecks/overloaded?threshold=${threshold}`, token);
}

export async function getReferralPatterns(token: string) {
  return apiFetch("/api/admin/intelligence/care-path/patterns", token);
}

export async function triggerGraphSync(token: string) {
  return apiFetch("/api/admin/intelligence/graph/sync", token, { method: "POST" });
}
