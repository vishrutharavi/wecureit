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

export async function createRoom(
  token: string,
  facilityId: string,
  payload: {
    roomName: string;
    roomNumber: string;
    specialityCode: string;
  }
) {
  return apiFetch(`/api/admin/facilities/${facilityId}/rooms`, token, {
    method: "POST",
    body: JSON.stringify(payload),
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
