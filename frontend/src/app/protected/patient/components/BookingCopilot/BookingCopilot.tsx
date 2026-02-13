"use client";

import React from "react";
import { useRouter } from "next/navigation";
import styles from "./bookingCopilot.module.scss";
import { apiFetch } from "@/lib/api";
import CopilotInput from "./CopilotInput";
import CopilotIntent from "./CopilotIntent";
import CopilotSuggestions from "./CopilotSuggestions";
import type { BookingIntent, SlotSuggestion, SuggestResponse } from "./CopilotState";
import type { BookingResponse } from "@/app/protected/patient/types";

const samplePrompts = [
  "Dermatology next week after 4pm for 30 minutes",
  "General practice on Friday morning",
  "Pediatrics at Downtown Clinic for 60 minutes",
];

function formatTimeLabel(input?: string | null) {
  if (!input) return null;
  const match = input.match(/(\d{1,2}):(\d{2})/);
  if (!match) return input;
  const hh = parseInt(match[1], 10);
  const mm = match[2];
  const isPM = hh >= 12;
  const displayH = hh % 12 === 0 ? 12 : hh % 12;
  return `${displayH}:${mm} ${isPM ? "PM" : "AM"}`;
}

export default function BookingCopilot() {
  const router = useRouter();
  const [utterance, setUtterance] = React.useState("");
  const [intent, setIntent] = React.useState<BookingIntent | null>(null);
  const [suggestions, setSuggestions] = React.useState<SlotSuggestion[]>([]);
  const [alternatives, setAlternatives] = React.useState<SlotSuggestion[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  type LookupItem = { id: string; name: string };
  type LookupState = { specialties: LookupItem[]; facilities: LookupItem[]; doctors: LookupItem[] };

  const [lookup, setLookup] = React.useState<LookupState>({
    specialties: [],
    facilities: [],
    doctors: [],
  });

  const normalize = (value?: string | null) => (value ?? "").trim().toLowerCase();
  const normalizeDoctorName = (value?: string | null) =>
    normalize(value).replace(/^dr\.?\s+/i, "");

    React.useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const resp = (await apiFetch("/api/patients/booking/dropdown-data")) as BookingResponse;
        if (!mounted || !resp) return;

        const specialties = Array.isArray(resp.specialties)
          ? resp.specialties.map((s) => ({ id: s.code, name: s.name }))
          : [];
        const facilities = Array.isArray(resp.facilities)
          ? resp.facilities.map((f) => ({ id: f.id, name: f.name }))
          : [];
        const doctors = Array.isArray(resp.doctors)
          ? resp.doctors.map((d) => ({
              id: d.id,
              name: d.displayName ?? d.name ?? "Doctor",
            }))
          : [];

        setLookup({ specialties, facilities, doctors });
      } catch {}
    })();

    return () => {
      mounted = false;
    };
  }, []);
    const resolveDoctorId = (slot: SlotSuggestion) =>
    slot.doctorId ??
    lookup.doctors.find(
      (d) => normalizeDoctorName(d.name) === normalizeDoctorName(slot.doctorName)
    )?.id ??
    null;

  const resolveDoctorName = (slot: SlotSuggestion) =>
    slot.doctorName ??
    lookup.doctors.find((d) => d.id === slot.doctorId)?.name ??
    null;

  const resolveFacilityId = (slot: SlotSuggestion) =>
    slot.facilityId ??
    lookup.facilities.find((f) => normalize(f.name) === normalize(slot.facilityName))?.id ??
    null;

  const resolveFacilityName = (slot: SlotSuggestion) =>
    slot.facilityName ??
    lookup.facilities.find((f) => f.id === slot.facilityId)?.name ??
    null;

  const resolveSpecialtyId = (slot: SlotSuggestion) =>
    slot.specialtyId ??  
    lookup.specialties.find(
      (s) =>
        normalize(s.name) === normalize(slot.specialty) ||
        normalize(s.id) === normalize(slot.specialty)
    )?.id ?? null;

  const resolveSpecialtyName = (slot: SlotSuggestion) =>
    slot.specialty ??
    lookup.specialties.find((s) => s.id === resolveSpecialtyId(slot))?.name ??
    null;

  const handleSubmit = React.useCallback(async () => {
    const trimmed = utterance.trim();
    if (!trimmed) {
      setError("Enter a request like: Dermatology next week after 4pm.");
      return;
    }

    setLoading(true);
    setError(null);
    setIntent(null);
    setSuggestions([]);
    setAlternatives([]);

    try {
      const parsed = await apiFetch("/api/agent/booking/interpret", undefined, {
        method: "POST",
        body: JSON.stringify({ utterance: trimmed }),
      });
      setIntent(parsed as BookingIntent);

      const suggest = await apiFetch("/api/agent/booking/suggest", undefined, {
        method: "POST",
        body: JSON.stringify(parsed),
      });

      const typed = suggest as SuggestResponse;
      setSuggestions(typed?.suggestions ?? []);
      setAlternatives(typed?.alternatives ?? []);
    } catch (err: unknown) {
        const message = err instanceof Error ? err.message : String(err ?? "");
        setError(message || "Could not fetch suggestions.");
    } finally {
      setLoading(false);
    }
  }, [utterance]);

  const handleClear = React.useCallback(() => {
    setUtterance("");
    setIntent(null);
    setSuggestions([]);
    setAlternatives([]);
    setError(null);
  }, []);

  const handleBook = React.useCallback(
    (slot: SlotSuggestion) => {
      if (!slot.date || !slot.startTime) {
        setError("Missing date/time on this suggestion.");
        return;
      }

      const duration = slot.durationMinutes ?? intent?.durationMinutes ?? 30;
      const bookingSelection = {
        doctor: { id: resolveDoctorId(slot), name: resolveDoctorName(slot) },
        facility: { id: resolveFacilityId(slot), name: resolveFacilityName(slot) },
        specialty: { id: resolveSpecialtyId(slot), name: resolveSpecialtyName(slot) },
        date: slot.date,
        time: formatTimeLabel(slot.startTime),
        duration: duration,
        doctorAvailabilityId: slot.doctorAvailabilityId ?? null,  // ADD THIS LINE
      };

      try {
        sessionStorage.setItem("bookingSelection", JSON.stringify(bookingSelection));
      } catch {}

      router.push("/protected/patient?tab=confirmation");
    },
    [router, intent] 
  );

  return (
    <section className={styles.panel}>
      <div className={styles.header}>
        <span className={styles.tag}>Booking Copilot</span>
        <h2 className={styles.title}>Say what you need. We will match it.</h2>
        <p className={styles.sub}>
          Describe the specialty, timing, or clinic and get real appointment slots.
        </p>
      </div>

      <CopilotInput
        utterance={utterance}
        loading={loading}
        error={error}
        samplePrompts={samplePrompts}
        onChange={setUtterance}
        onSubmit={handleSubmit}
        onClear={handleClear}
        onPickPrompt={setUtterance}
      />

      {(intent || suggestions.length > 0 || alternatives.length > 0) && (
        <div className={styles.output}>
          {intent && <CopilotIntent intent={intent} />}

          <CopilotSuggestions
            title="Top suggestions"
            emptyText="No exact matches. Check alternatives below."
            suggestions={suggestions}
            badgeLabel="Suggested"
            defaultDuration={intent?.durationMinutes ?? 30}
            onBook={handleBook}
          />

          {alternatives.length > 0 && (
            <CopilotSuggestions
              title="Alternatives"
              emptyText="No alternatives yet."
              suggestions={alternatives}
              badgeLabel="Alternative"
              badgeAlt
              defaultDuration={intent?.durationMinutes ?? 30}
              onBook={handleBook}
            />
          )}
        </div>
      )}
    </section>
  );
}