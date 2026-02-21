import { apiFetch } from "../api";

/* ---------- Cards ---------- */

export async function getCards(patientId: string, token?: string) {
  return apiFetch(`/cards/getcards?patientId=${patientId}`, token);
}

export async function addCard(
  payload: {
    pan: string;
    cvc: string;
    expMonth: number;
    expYear: number;
    patientMasterId: string | null;
  },
  token?: string
) {
  return apiFetch("/cards/add", token, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function deleteCard(
  cardId: string,
  patientId: string,
  token?: string
) {
  return apiFetch(`/cards/${cardId}?patientId=${patientId}`, token, {
    method: "DELETE",
  });
}
