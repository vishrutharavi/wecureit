"use client";

import styles from "../doctor.module.scss";
import { Calendar, Clock, FileText } from "lucide-react";

type Tab = "schedule" | "availability" | "notes";

type Props = {
  active: Tab;
  onChange: (tab: Tab) => void;
};

export default function DoctorTabs({ active, onChange }: Props) {
  return (
    <div className={styles.tabs}>
      <button
        className={`${styles.tab} ${active === "schedule" ? styles.active : ""}`}
        onClick={() => onChange("schedule")}
      >
        <Calendar size={16} /> My Schedule
      </button>

      <button
        className={`${styles.tab} ${active === "availability" ? styles.active : ""}`}
        onClick={() => onChange("availability")}
      >
        <Clock size={16} /> Set Availability
      </button>

      <button
        className={`${styles.tab} ${active === "notes" ? styles.active : ""}`}
        onClick={() => onChange("notes")}
      >
        <FileText size={16} /> Appointments & Notes
      </button>
    </div>
  );
}

