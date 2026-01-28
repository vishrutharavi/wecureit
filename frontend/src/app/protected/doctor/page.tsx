"use client";

import { useState, useEffect } from "react";
import styles from "./doctor.module.scss";
import DoctorHeader from "./components/DoctorHeader";
import DoctorTabs from "./components/DoctorTabs";
import ScheduleView from "./components/Schedule/ScheduleView";
import AvailabilityView from "./components/Availability/AvailabilityView";
import NotesView from "./components/Notes/NotesView";


export default function DoctorPage() {
  const [tab, setTab] = useState<"schedule" | "availability" | "notes">("schedule");
  const [doctorName, setDoctorName] = useState<string | undefined>(undefined);

  useEffect(() => {
    try {
      const raw = localStorage.getItem('doctorProfile');
      if (raw) {
        const obj = JSON.parse(raw);
        // defer to avoid synchronous setState in effect
        setTimeout(() => setDoctorName(obj.name ?? obj.email ?? undefined), 0);
      }
    } catch {}
  }, []);

  return (
    <div className={styles.wrapper}>
  <DoctorHeader doctorName={doctorName} />
      <DoctorTabs active={tab} onChange={setTab} />

      {tab === "schedule" && <ScheduleView />}
      {tab === "availability" && <AvailabilityView />}
      {tab === "notes" && <NotesView />}
    </div>
  );
}
