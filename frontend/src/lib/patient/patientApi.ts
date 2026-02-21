import { apiFetch } from "../api";

/* ---------- Booking Dropdown ---------- */

export async function getBookingDropdownData(params?: URLSearchParams) {
  const qs = params?.toString();
  const url = "/api/patients/booking/dropdown-data" + (qs ? `?${qs}` : "");
  return apiFetch(url);
}

/* ---------- Booking Availability ---------- */

export async function getBookingAvailability(params: URLSearchParams) {
  return apiFetch(`/api/patients/booking/availability?${params.toString()}`);
}

/* ---------- Optimal Slot Suggestions ---------- */

export async function suggestOptimalSlots(payload: {
  doctorId: string;
  facilityId: string;
  specialtyCode?: string | null;
  duration: number;
}) {
  return apiFetch("/api/patients/booking/suggest-optimal-slots", undefined, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/* ---------- Appointments ---------- */

export async function createAppointment(body: Record<string, unknown>) {
  return apiFetch("/appointments/create", undefined, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getUpcomingAppointments(patientId: string) {
  return apiFetch(`/appointments/byPatient?patientId=${patientId}`);
}

export async function cancelAppointment(
  appointmentId: string,
  token?: string | null
) {
  return apiFetch(
    `/appointments/${appointmentId}/cancel?cancelledBy=patient`,
    token ?? undefined,
    { method: "POST" }
  );
}

export async function getAppointmentHistory(patientId: string) {
  return apiFetch(
    `/appointments/history/byPatient?patientId=${patientId}`
  );
}

/* ---------- Booking Copilot / Agent ---------- */

export async function interpretBookingUtterance(utterance: string) {
  return apiFetch("/api/agent/booking/interpret", undefined, {
    method: "POST",
    body: JSON.stringify({ utterance }),
  });
}

export async function suggestBookingSlots(intent: Record<string, unknown>) {
  return apiFetch("/api/agent/booking/suggest", undefined, {
    method: "POST",
    body: JSON.stringify(intent),
  });
}

/* ---------- Patient Referrals ---------- */

export async function getPatientReferrals(patientId: string) {
  return apiFetch(`/api/patient/${patientId}/referrals`);
}
