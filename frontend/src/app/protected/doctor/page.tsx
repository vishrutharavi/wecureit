"use client";

import { useState } from "react";
import styles from "./doctor.module.scss";
import DoctorHeader from "./components/DoctorHeader";
import DoctorTabs from "./components/DoctorTabs";
import ScheduleView from "./components/Schedule/ScheduleView";
import AvailabilityView from "./components/Availability/AvailabilityView";
import NotesView from "./components/Notes/NotesView";


export default function DoctorPage() {
  const [tab, setTab] = useState<"schedule" | "availability" | "notes">("schedule");

  return (
    <div className={styles.wrapper}>
      <DoctorHeader />
      <DoctorTabs active={tab} onChange={setTab} />

      {tab === "schedule" && <ScheduleView />}
      {tab === "availability" && <AvailabilityView />}
      {tab === "notes" && <NotesView />}
    </div>
  );
}
