import { apiFetch } from "../api";

/* ---------- Auth Link ---------- */

export async function linkIdentity(token: string, portal: "PATIENT" | "DOCTOR" | "ADMIN") {
  return apiFetch("/api/auth/link", token, {
    method: "POST",
    body: JSON.stringify({ portal }),
  });
}

/* ---------- Patient Profile ---------- */

export async function getPatientMe(token?: string) {
  return apiFetch("/api/patient/me", token);
}

export async function updatePatientProfile(payload: {
  email?: string;
  name?: string;
  phone?: string;
}) {
  return apiFetch("/api/patient/profile", undefined, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/* ---------- Doctor Profile ---------- */

export async function getDoctorMe(token?: string) {
  return apiFetch("/api/doctor/me", token);
}

/* ---------- Admin Profile ---------- */

export async function getAdminMe(token: string) {
  return apiFetch("/api/admin/me", token);
}

/* ---------- Signup ---------- */

export async function signup(payload: Record<string, unknown>) {
  return apiFetch("/api/auth/signup", undefined, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
