"use client";

import React from "react";
import styles from "./bookingCopilot.module.scss";
import type { BookingIntent } from "./CopilotState";

export default function CopilotIntent({ intent }: { intent: BookingIntent }) {
  const normalizeDate = (value?: string | null) => {
    if (!value) return value;
    if (value.includes("T")) return value.split("T")[0];
    return value;
  };

  const normalizeTime = (value?: string | null) => {
    if (!value) return value;
    if (value.includes("T")) {
      const timePart = value.split("T")[1] || "";
      return timePart.slice(0, 5);
    }
    if (value.length >= 5 && value[2] === ":") return value.slice(0, 5);
    return value;
  };

  const items = [
    { label: "Specialty", value: intent.specialty },
    { label: "Facility", value: intent.facilityName },
    { label: "Doctor", value: intent.doctorName },
    { label: "Date Start", value: normalizeDate(intent.preferredDateStart) },
    { label: "Date End", value: normalizeDate(intent.preferredDateEnd) },
    { label: "Time Start", value: normalizeTime(intent.preferredTimeStart) },
    { label: "Time End", value: normalizeTime(intent.preferredTimeEnd) },
    {
      label: "Duration",
      value: intent.durationMinutes ? `${intent.durationMinutes} min` : null,
    },
  ];

  return (
    <div>
      <div className={styles.sectionTitle}>Interpreted intent</div>
      <div className={styles.intentGrid}>
        {items.map((item) => (
          <div key={item.label} className={styles.intentCard}>
            <div className={styles.intentLabel}>{item.label}</div>
            <div className={styles.intentValue}>{item.value || "—"}</div>
          </div>
        ))}
      </div>
    </div>
  );
}