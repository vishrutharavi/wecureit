import { apiFetch } from "../api";

/* ---------- Facilities ---------- */

export async function getDoctorFacilities(doctorId: string, token?: string) {
  return apiFetch(`/api/doctors/${doctorId}/facilities`, token);
}

/* ---------- Availability ---------- */

export async function getDoctorAvailability(
  doctorId: string,
  params?: { from?: string; to?: string },
  token?: string
) {
  const qs = new URLSearchParams();
  if (params?.from) qs.set("from", params.from);
  if (params?.to) qs.set("to", params.to);
  const query = qs.toString();
  return apiFetch(
    `/api/doctors/${doctorId}/availability${query ? `?${query}` : ""}`,
    token
  );
}

export async function createDoctorAvailability(
  doctorId: string,
  payload: Array<{
    workDate: string;
    startTime: string;
    endTime: string;
    specialityCode?: string;
    facilityId: string | null;
  }>,
  token?: string
) {
  return apiFetch(`/api/doctors/${doctorId}/availability`, token, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function deleteDoctorAvailability(
  doctorId: string,
  availabilityId: string,
  token?: string
) {
  return apiFetch(
    `/api/doctors/${doctorId}/availability/${availabilityId}/delete-availability`,
    token,
    { method: "DELETE" }
  );
}

export async function getLockedAvailabilities(
  doctorId: string,
  workDate: string,
  token?: string
) {
  return apiFetch(
    `/api/doctors/${doctorId}/locked-availabilities?workDate=${workDate}`,
    token
  );
}

export async function getFacilityAvailability(
  doctorId: string,
  facilityId: string,
  params: { workDate: string; start: string; end: string },
  token?: string
) {
  return apiFetch(
    `/api/doctors/${doctorId}/facilities/${facilityId}/availability?workDate=${params.workDate}&start=${params.start}&end=${params.end}`,
    token
  );
}

/* ---------- Schedule ---------- */

export async function getDoctorSchedule(
  doctorId: string,
  date: string,
  token?: string
) {
  return apiFetch(`/api/doctors/${doctorId}/schedule?date=${date}`, token);
}

/* ---------- Appointments ---------- */

export async function completeAppointment(
  doctorId: string,
  appointmentId: string,
  token?: string
) {
  return apiFetch(
    `/api/doctors/${doctorId}/appointments/${appointmentId}/complete`,
    token,
    { method: "POST" }
  );
}

export async function getCompletedAppointments(
  doctorId: string,
  startDate: string,
  endDate: string,
  token?: string
) {
  return apiFetch(
    `/api/doctors/${doctorId}/completed-appointments?startDate=${startDate}&endDate=${endDate}`,
    token
  );
}

/* ---------- Clinical Notes ---------- */

export async function createClinicalNote(
  payload: Record<string, unknown>,
  token?: string
) {
  return apiFetch("/api/clinical-notes", token, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getClinicalNotes(
  params: { appointmentDbId?: string; patientId?: string },
  token?: string
) {
  const qs = new URLSearchParams();
  if (params.appointmentDbId)
    qs.set("appointmentDbId", params.appointmentDbId);
  else if (params.patientId) qs.set("patientId", params.patientId);
  const query = qs.toString();
  return apiFetch(
    `/api/clinical-notes${query ? `?${query}` : ""}`,
    token
  );
}
