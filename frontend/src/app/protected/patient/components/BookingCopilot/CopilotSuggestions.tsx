"use client";

import React from "react";
import styles from "./bookingCopilot.module.scss";
import type { SlotSuggestion } from "./CopilotState";

type Props = {
  title: string;
  emptyText: string;
  suggestions: SlotSuggestion[];
  badgeLabel: string;
  badgeAlt?: boolean;
  defaultDuration?: number;
  onBook: (slot: SlotSuggestion) => void;
};

export default function CopilotSuggestions({
  title,
  emptyText,
  suggestions,
  badgeLabel,
  badgeAlt,
  defaultDuration,
  onBook,
}: Props) {
    const computeEndTime = (start: string | null | undefined, duration: number) => {
    if (!start) return null;
    const m = start.match(/(\d{1,2}):(\d{2})/);
    if (!m) return null;
    const hh = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);
    const total = hh * 60 + mm + duration;
    const endH = Math.floor(total / 60) % 24;
    const endM = total % 60;
    return `${String(endH).padStart(2, "0")}:${String(endM).padStart(2, "0")}`;
  };
  return (
    <div>
      <div className={styles.sectionTitle}>{title}</div>
      <div className={styles.list}>
        {suggestions.length === 0 ? (
          <div className={styles.empty}>{emptyText}</div>
        ) : (
          suggestions.map((slot, index) => (
            <div key={`${slot.doctorId}-${index}`} className={styles.card}>
              <div className={styles.cardHeader}>
                <div className={styles.cardTitle}>
                  {slot.doctorName || "Doctor"} · {slot.specialty || "Specialty"}
                </div>
                <span className={badgeAlt ? styles.badgeAlt : styles.badge}>
                  {badgeLabel}
                </span>
              </div>

              <div className={styles.meta}>
                <span>{slot.date || "Date TBD"}</span>
                <span>
                  {slot.startTime || "--:--"} - {computeEndTime(slot.startTime, slot.durationMinutes ?? defaultDuration ?? 30) || "--:--"}
                </span>
                <span>{slot.facilityName || "Any facility"}</span>
              </div>

              <div className={styles.reason}>{slot.reason || "Matches your request."}</div>

              <button className={styles.bookButton} onClick={() => onBook(slot)}>
                Book this slot
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}