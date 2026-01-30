"use client";

import React, { useMemo, useState } from "react";
import styles from "@/app/protected/patient/patient.module.scss";
import { FiSearch, FiCalendar, FiClock, FiMapPin, FiFileText } from "react-icons/fi";

type Appointment = {
  id: string;
  doctor: string;
  specialty: string;
  dateLabel: string; // e.g. Tue, Oct 14, 2025
  timeLabel: string; // e.g. 9:00 AM - 9:30 AM
  location: string;
  remarks: string;
  status: "completed" | "cancelled";
};

const sampleAppointments: Appointment[] = [
  {
    id: "a1",
    doctor: "Dr. Sarah Johnson",
    specialty: "Cardiology",
    dateLabel: "Tue, Oct 14, 2025",
    timeLabel: "9:00 AM - 9:30 AM",
    location: "Downtown Medical Center",
    remarks: "Regular checkup. Blood pressure normal. Prescribed medication for cholesterol management.",
    status: "completed",
  },
  {
    id: "a2",
    doctor: "Dr. Michael Chen",
    specialty: "Orthopedics",
    dateLabel: "Sun, Sep 21, 2025",
    timeLabel: "2:00 PM - 2:45 PM",
    location: "Alexandria Main Hospital",
    remarks: "Follow-up for knee pain. Recommended physical therapy sessions. Patient showing improvement.",
    status: "completed",
  },
];

export default function AppointmentHistory() {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<"all" | "completed" | "cancelled">("all");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const matched = sampleAppointments.filter((a) => {
      if (filter !== "all" && a.status !== filter) return false;
      if (!q) return true;
      return (
        a.doctor.toLowerCase().includes(q) ||
        a.specialty.toLowerCase().includes(q) ||
        a.location.toLowerCase().includes(q) ||
        a.remarks.toLowerCase().includes(q)
      );
    });

    // sort recent to oldest based on local datetime parsed from dateLabel + timeLabel
    return matched.sort((a, b) => {
      try {
        const aStart = a.timeLabel.split(" - ")[0];
        const bStart = b.timeLabel.split(" - ")[0];
        const ta = new Date(`${a.dateLabel} ${aStart}`).getTime();
        const tb = new Date(`${b.dateLabel} ${bStart}`).getTime();
        return tb - ta;
      } catch {
        return 0;
      }
    });
  }, [query, filter]);

  return (
    <div>
      <div className={styles.ahHeader}>
        <h2 className={styles.profileTitle}>Appointment History</h2>
        <div className={styles.subtitle}>View your past appointments and medical records</div>
      </div>

      <div className={`${styles.panelWhite} ${styles.panelTopSpacing}`}>
        <div className={styles.searchRow}>
          <div className={styles.searchLeft}>
            <div className={styles.searchInputWrap}>
              <FiSearch className={styles.iconMuted} />
              <input
                className={styles.inputField}
                placeholder="Search by doctor, specialty, facility, or remarks..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </div>

          <div className={styles.filtersRow}>
            <button
              onClick={() => setFilter("all")}
              className={filter === "all" ? styles.editProfileBtn : `${styles.filterPill} ${styles.filterInactive}`}
            >
              All
            </button>
            <button
              onClick={() => setFilter("completed")}
              className={filter === "completed" ? styles.editProfileBtn : `${styles.filterPill} ${styles.filterInactive}`}
            >
              Completed
            </button>
            <button
              onClick={() => setFilter("cancelled")}
              className={filter === "cancelled" ? styles.editProfileBtn : `${styles.filterPill} ${styles.filterInactive}`}
            >
              Cancelled
            </button>
          </div>
        </div>
      </div>

      <div className={styles.listWrap}>
        {filtered.map((a) => (
          <div key={a.id} className={styles.appointmentCard}>
            <div className={styles.appointmentHeader}>
              <div>
                <div className={styles.doctorRow}>
                  <h3>{a.doctor}</h3>
                  <div className={styles.smallBadge}>{a.specialty}</div>
                </div>

                <div className={styles.metaRow}>
                  <div className={styles.metaItem}>
                    <FiCalendar /> <div className={styles.mutedText}>{a.dateLabel}</div>
                  </div>

                  <div className={styles.metaItem}>
                    <FiClock /> <div className={styles.mutedText}>{a.timeLabel}</div>
                  </div>

                  <div className={styles.metaItem}>
                    <FiMapPin /> <div className={styles.mutedText}>{a.location}</div>
                  </div>
                </div>
              </div>

              <div>
                <div className={`${styles.statusBadge} ${a.status === "completed" ? styles.statusCompleted : a.status === "cancelled" ? styles.statusCancelled : ""}`}>
                  {a.status === "completed" ? "Completed" : a.status === "cancelled" ? "Cancelled" : ""}
                </div>
              </div>
            </div>

            <hr className={styles.divider} />

            <div className={styles.remarksRow}>
              <div className={styles.iconMuted}><FiFileText /></div>
              <div>
                <div className={styles.remarksTitle}>Remarks:</div>
                <div className={styles.remarksText}>{a.remarks}</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

